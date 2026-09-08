#!/bin/bash
#
# Start (or stop) every runnable Spring Boot microservice in this repo.
#
# Path-agnostic: the repo root is derived from this script's own location,
# so it works from any checkout on any machine.
#
#   ./start_all_services.sh          start all services
#   ./start_all_services.sh --stop   stop everything started by this script
#   ./start_all_services.sh --status show which services are up
#
# Requires: a JDK 21 and Maven on PATH. Redis + Kafka must already be
# running on localhost:6379 / localhost:9092 (see docker-compose.yml).
#
# Services are built on demand into executable "fat" jars. The pom only
# declares spring-boot-maven-plugin without a repackage execution, so the
# jar must be repackaged explicitly - this script does that for you.

set -u

# --- locate repo root (directory containing this script) --------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR"
IMPL_DIR="$REPO_ROOT/KafkaSpringbootImplementation"
LOG_DIR="$REPO_ROOT/logs"
PID_DIR="$LOG_DIR/pids"

# --- pick a JDK 21 ------------------------------------------------------
# The services target Java 21 (maven.compiler.* = 21) and Spring Boot 3.1
# needs 17+. Note: `/usr/libexec/java_home -v 21` on macOS falls back to the
# highest installed JDK when 21 is absent, so its result must be verified.
is_jdk21() {
    [[ -x "$1/bin/java" ]] && "$1/bin/java" -version 2>&1 | grep -qE '"21[.\"]|version "21'
}

if [[ -n "${JAVA_HOME:-}" ]] && is_jdk21 "$JAVA_HOME"; then
    :  # caller already set a valid JDK 21
else
    JAVA_HOME=""
    for cand in \
        "$(/usr/libexec/java_home -v 21 2>/dev/null || true)" \
        /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/*21*/Contents/Home \
        /Library/Java/JavaVirtualMachines/temurin-21*/Contents/Home
    do
        if [[ -n "$cand" ]] && is_jdk21 "$cand"; then
            JAVA_HOME="$cand"
            break
        fi
    done
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "ERROR: no JDK 21 found. Install one, e.g.:  brew install openjdk@21" >&2
    echo "       then re-run, or:  JAVA_HOME=/path/to/jdk-21 $0" >&2
    exit 1
fi
export JAVA_HOME
JAVA_BIN="$JAVA_HOME/bin/java"

# --- locate maven --------------------------------------------------------
MVN_BIN="$(command -v mvn || true)"
for cand in /opt/homebrew/bin/mvn /usr/local/bin/mvn; do
    [[ -z "$MVN_BIN" && -x "$cand" ]] && MVN_BIN="$cand"
done

# --- service list: every subdir of KafkaSpringbootImplementation with a pom -
list_services() {
    for d in "$IMPL_DIR"/*/; do
        [[ -f "$d/pom.xml" ]] && basename "$d"
    done
}

boot_jar() {
    # echo the path to the runnable jar for a service dir, if one exists
    ls "$1"/target/*.jar 2>/dev/null | grep -Ev '\.original$|-sources\.jar$|-javadoc\.jar$' | head -n 1
}

is_fat_jar() {
    unzip -l "$1" 2>/dev/null | grep -q 'BOOT-INF/'
}

svc_port() {
    # server.port from the service's application.properties (empty if unset)
    local props="$IMPL_DIR/$1/src/main/resources/application.properties"
    [[ -f "$props" ]] && sed -n 's/^[[:space:]]*server\.port[[:space:]]*=[[:space:]]*//p' "$props" | tail -n 1
}

port_in_use() {
    [[ -n "$1" ]] && lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

pid_alive() {
    [[ -f "$1" ]] && kill -0 "$(cat "$1" 2>/dev/null)" 2>/dev/null
}

# ---------------------------------------------------------------------------
case "${1:-start}" in
    --stop|stop)
        echo "Stopping services..."
        for name in $(list_services); do
            pidfile="$PID_DIR/$name.pid"
            port="$(svc_port "$name")"
            stopped=0

            # 1) kill the pid we recorded, if it's still alive
            if pid_alive "$pidfile"; then
                pid="$(cat "$pidfile")"
                kill "$pid" 2>/dev/null && echo "  stopped $name (pid $pid)" && stopped=1
            fi
            rm -f "$pidfile"

            # 2) fallback: something is still on the service's port (stale/lost
            #    pidfile from an earlier run) - kill the listener too
            if [[ "$stopped" -eq 0 ]] && port_in_use "$port"; then
                lp="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null)"
                for p in $lp; do
                    kill "$p" 2>/dev/null && echo "  stopped $name (pid $p, found by port $port)" && stopped=1
                done
            fi

            [[ "$stopped" -eq 0 ]] && echo "  $name not running"
        done
        exit 0
        ;;
    --status|status)
        for name in $(list_services); do
            pidfile="$PID_DIR/$name.pid"
            port="$(svc_port "$name")"
            if pid_alive "$pidfile"; then
                echo "  UP    $name (pid $(cat "$pidfile")${port:+, port $port})"
            elif port_in_use "$port"; then
                echo "  UP?   $name (port $port in use, but not by this script)"
            else
                echo "  down  $name"
            fi
        done
        exit 0
        ;;
    --start|start)
        ;;
    *)
        echo "usage: $0 [--start|--stop|--status]" >&2
        exit 2
        ;;
esac

# --- start ---------------------------------------------------------------
mkdir -p "$LOG_DIR" "$PID_DIR"
echo "Repo root : $REPO_ROOT"
echo "Java      : $JAVA_BIN"
echo "Maven     : ${MVN_BIN:-<not found>}"
echo "Starting all microservices..."
echo

started=0
skipped=0
running=0
for name in $(list_services); do
    svc_dir="$IMPL_DIR/$name"
    port="$(svc_port "$name")"
    pidfile="$PID_DIR/$name.pid"

    # guard: don't launch a second copy of something already up
    if pid_alive "$pidfile"; then
        echo "  ok    $name already running (pid $(cat "$pidfile"))"
        running=$((running + 1))
        continue
    fi
    if port_in_use "$port"; then
        echo "  ok    $name port $port already in use - leaving it alone (stale pidfile? run --stop or free the port)"
        running=$((running + 1))
        continue
    fi
    # a dead pidfile from a previous run - clear it so a fresh pid is written
    [[ -f "$pidfile" ]] && rm -f "$pidfile"

    jar="$(boot_jar "$svc_dir")"

    needs_build=0
    if [[ -z "$jar" ]]; then
        needs_build=1
    elif ! is_fat_jar "$jar"; then
        needs_build=1
    fi

    # build / repackage on demand
    if [[ "$needs_build" -eq 1 ]]; then
        if [[ -z "$MVN_BIN" ]]; then
            echo "  skip  $name (no runnable jar and mvn not found - run 'mvn -DskipTests package' then repackage)"
            skipped=$((skipped + 1))
            continue
        fi
        echo "  build $name ..."
        if ! "$MVN_BIN" -q -DskipTests -f "$svc_dir/pom.xml" \
                package org.springframework.boot:spring-boot-maven-plugin:repackage \
                > "$LOG_DIR/$name.build.log" 2>&1; then
            echo "  skip  $name (build failed - see $LOG_DIR/$name.build.log)"
            skipped=$((skipped + 1))
            continue
        fi
        jar="$(boot_jar "$svc_dir")"
    fi

    if [[ -z "$jar" ]]; then
        echo "  skip  $name (no jar after build)"
        skipped=$((skipped + 1))
        continue
    fi

    log_file="$LOG_DIR/$name.log"
    nohup "$JAVA_BIN" -jar "$jar" > "$log_file" 2>&1 &
    echo "$!" > "$pidfile"
    echo "  start $name (pid $!, log $log_file)"
    started=$((started + 1))
done

echo
echo "Started $started service(s), already running $running, skipped $skipped."
echo "Check with: $0 --status   |   stop with: $0 --stop"

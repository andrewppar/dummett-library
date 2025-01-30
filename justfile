default:
    @just --list

clean:
    rm -rf result

build:
    nix build .\#all

run: deps build
    process-compose -f build/process-compose.yml -t=false up backend/run frontend/run

deps:
	nix run github:jlesquembre/clj-nix#deps-lock

dev target:
    #!/bin/bash
    case {{target}} in
        frontend)
            process-compose -f build/process-compose.yml -t=false up backend/run frontend/dev
            ;;
        backend)
            process-compose -f build/process-compose.yml -t=false up backend/dev
    esac

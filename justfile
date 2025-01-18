default:
    @just --list

clean:
    rm -rf result

build:
    nix build .\#all

run target: build
    #!/bin/bash
    case {{target}} in
        frontend)
            ./result/bin/dummett_library_frontend
            ;;
        backend)
            ./result/bin/dummett_library_backend
            ;;
        *)
            printf "\033[0;31mERROR:\033[0m Must run either"
            printf "'frontend' or 'backend'. "
            printf "Got {{target}}\n"
            ;;
    esac

dev target:
    #!/bin/bash
    case {{target}} in
        frontend)
            npx shadow-cljs watch app
            ;;
        backend)
            clj -M:dev/repl
            ;;
        *)
            printf "\033[0;31mERROR:\033[0m Must run either"
            printf "'frontend' or 'backend'. "
            printf "Got {{target}}\n"
            ;;
    esac

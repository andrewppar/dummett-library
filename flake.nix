{description = "search the Dummett corpus" ;
 inputs = {
   nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable" ;
   flake-utils.url = "github:numtide/flake-utils";
 } ;
 outputs = {self, nixpkgs, flake-utils, ... }:
   flake-utils.lib.eachDefaultSystem (system:
     let pkgs = import nixpkgs { inherit system; } ;
         pname = "dummett-library" ;
         pversion = "0.1.1";
         backend = import ./build/backend.nix {
           pkgs = pkgs ;
           package-name = pname ;
           package-version = pversion ;
         };
         frontend = import ./build/frontend.nix {
           pkgs = pkgs ;
           package-name = pname ;
           package-version = pversion ;
         } ;
         all-packages = pkgs.symlinkJoin {
           name = "all" ;
           paths = [backend.dummett-library frontend.dummett-library] ;
         } ;
     in {
       packages.backend = backend.dummett-library ;
       packages.frontend = frontend.dummett-library ;
       packages.default = all-packages ;

       #devShells.default =
       #  let
       #    shell-fn = {name, commands}:
       #      "function " + name + " () {\n"
       #      + (builtins.foldl' (acc: elem: acc + "  " + elem + "\n") "" commands)
       #      + "}\n" ;
       #    fns = builtins.concatStringsSep "\n" [
       #      (shell-fn {
       #        name = "clean" ;
       #        commands = [
       #          ''echo "cleaning repo..."''
       #          ''rm -rf result''
       #          ''rm -rf outputs''
       #          ''rm -rf target''
       #        ];
       #      })
       #      (shell-fn {
       #        name = "deps-lock" ;
       #        commands =
       #          ["nix run github:jlesquembre/clj-nix#deps-lock"];})
       #      (shell-fn {
       #        name = "build";
       #        commands =
       #          [''echo "building..."''] ++ backend-build-commands;
       #      })
       #      (shell-fn {
       #        name = "dev" ;
       #        commands = [''clojure -M:dev/repl &'' ''echo $! > pids.txt''] ;})
       #      (shell-fn {
       #        name = "install" ;
       #        commands = [''echo "installing..."''] ++ backend-install-commands ;
       #      })
       #      (shell-fn {
       #        name = "prod" ;
       #        commands = [
       #          ''RUN_TYPE=$1''
       #          ''if [[ -z $RUN_TYPE ]] ; then''
       #          ''  echo "Running clojure..."''
       #          ''  clj -X:prod/run''
       #          ''elif [[ $RUN_TYPE = "clj" ]] ; then''
       #          ''  echo "Running clojure..."''
       #          ''  clj -X:prod/run''
       #          ''else''
       #          ''  echo "Running as jar file..."''
       #          ''  clean''
       #          ''  build''
       #          ''  install''
       #          ''  java -cp outputs/out/${pname}-${pversion}-standalone.jar dummett_library.core''
       #          ''fi''
       #        ];
       #      })
       #      (shell-fn {
       #        name = "quit" ;
       #        commands = [
       #          ''for PID in $(cat pids.txt); do ''
       #          ''  kill $PID''
       #          ''done''
       #        ];})
       #      (shell-fn {name = "run" ; commands = ["build" "install" "java --version"];})
       #    ] ;
       #  in
       #    pkgs.mkShell {
       #      DATA_DIR="/Users/andrew/Dropbox/Dummett writings" ;
       #      INDEX_LOCATION="/opt/dummett";
       #      packages = frontend-build-dependencies ;
       #      shellHook = fns + '' echo "go forth and search..."'';
       #    } ;
     });
}

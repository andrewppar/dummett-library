{description = "search the Dummett corpus" ;
 inputs = {
   nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable" ;
   flake-utils.url = "github:numtide/flake-utils";
 } ;
 outputs = {self, nixpkgs, flake-utils, ... }:
   flake-utils.lib.eachDefaultSystem (system:
     let pkgs = import nixpkgs { inherit system; };
         lock = builtins.fromJSON (builtins.readFile ./deps-lock.json) ;
         to-url = segments:
           pkgs.lib.pipe
             segments
             [
               (map (pkgs.lib.removeSuffix "/"))
               (map (pkgs.lib.removePrefix "/"))
               (pkgs.lib.concatStringsSep "/")
             ];
         make-dep = mvn-dep:
           let
             mvn-path = mvn-dep.mvn-path ;
             path = pkgs.fetchurl {
               hash = mvn-dep.hash ;
               url = to-url [ mvn-dep.mvn-repo mvn-path ] ;
             } ;
           in
             { path = path ; name = mvn-path ; } ;
         mvn-cache = pkgs.linkFarm "maven-cache" (map make-dep lock.mvn-deps) ;
         dotclojure = pkgs.runCommand "dotclojure" { }
           ''
             mkdir -p $out/tools
             echo "{}" > $out/deps.edn
             echo "{}" > $out/tools/tools.edn
           '';
         deps-cache =  pkgs.linkFarm "clj-cache"
           [
             {
               name = ".m2/repository" ;
               path = mvn-cache ;
             }
             {
               name = ".clojure" ;
               path = dotclojure ;
             }
           ] ;
         build-dependencies =  with pkgs ; [ openjdk clojure git makeWrapper] ;
         build-commands = [''clj -T:prod/build uber''] ;
         install-commands =
           [
             ''mkdir -p $out''
             ''jarPath="$(find target -type f -name "*.jar" -print | head -n 1)"''
             ''cp $jarPath $out''
           ] ;
         pname = "dummett-library" ;
         pversion = "0.1.1";
     in {
       packages.default = pkgs.stdenv.mkDerivation {
         name = pname;
         version = pversion;
         src = ./.;
         nativeBuildInputs = build-dependencies;
         buildPhase = builtins.concatStringsSep "\n"
           (
             [
               ''export HOME="${deps-cache}"''
               ''export JAVA_TOOL_OPTIONS="-Duser.home=${deps-cache}"''
             ]
             ++ build-commands
           ) ;
         installPhase = builtins.concatStringsSep "\n"
           (install-commands
            ++
            [
              ''makeWrapper ${pkgs.openjdk}/bin/java $out/bin/dummett_library --add-flags "-cp $out/${pname}-${pversion}-standalone.jar" --add-flags dummett_library.core''
            ])
         ;
       } ;
       devShells.default =
         let
           shell-fn = {name, commands}:
             "function " + name + " () {\n"
             + (builtins.foldl' (acc: elem: acc + "  " + elem + "\n") "" commands)
             + "}\n" ;
           fns = builtins.concatStringsSep "\n" [
             (shell-fn {
               name = "clean" ;
               commands = [
                 ''echo "cleaning repo..."''
                 ''rm -rf result''
                 ''rm -rf outputs''
                 ''rm -rf target''
               ];
             })
             (shell-fn {
               name = "deps-lock" ;
               commands =
                 ["nix run github:jlesquembre/clj-nix#deps-lock"];})
             (shell-fn {
               name = "build";
               commands =
                 [''echo "building..."''] ++ build-commands;
             })
             (shell-fn {
               name = "dev" ;
               commands = [''clojure -M:dev/repl &'' ''echo $! > pids.txt''] ;})
             (shell-fn {
               name = "install" ;
               commands = [''echo "installing..."''] ++ install-commands ;
             })
             (shell-fn {
               name = "prod" ;
               commands = [
                 ''RUN_TYPE=$1''
                 ''if [[ -z $RUN_TYPE ]] ; then''
                 ''  echo "Running clojure..."''
                 ''  clj -X:prod/run''
                 ''elif [[ $RUN_TYPE = "clj" ]] ; then''
                 ''  echo "Running clojure..."''
                 ''  clj -X:prod/run''
                 ''else''
                 ''  echo "Running as jar file..."''
                 ''  clean''
                 ''  build''
                 ''  install''
                 ''  java -cp outputs/out/${pname}-${pversion}-standalone.jar dummett_library.core''
                 ''fi''
               ];
             })
             (shell-fn {
               name = "quit" ;
               commands = [
                 ''for PID in $(cat pids.txt); do ''
                 ''  kill $PID''
                 ''done''
               ];})
             (shell-fn {name = "run" ; commands = ["build" "install" "java --version"];})
           ] ;
         in
           pkgs.mkShell {
             DATA_DIR="/Users/andrew/Dropbox/Dummett writings" ;
             INDEX_LOCATION="/opt/dummett";
             packages = build-dependencies ;
             shellHook = fns + '' echo "go forth and search..."'';
           } ;
     }) ;
}

{pkgs, package-name, package-version}:
let lock = builtins.fromJSON (builtins.readFile ../deps-lock.json) ;
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
    build-deps =  with pkgs ; [ openjdk clojure git makeWrapper just] ;
    build-commands = [''clj -T:prod/build uber''] ;
    install-commands =
      [
        ''mkdir -p $out''
        ''jarPath="$(find target -type f -name "*.jar" -print | head -n 1)"''
        ''cp $jarPath $out''
      ] ;
    wrapper-command = builtins.concatStringsSep " "
      [
        ''makeWrapper''
        ''${pkgs.openjdk}/bin/java''
        ''$out/bin/dummett_library_backend''
        ''--add-flags "-cp $out/${package-name}-${package-version}-standalone.jar"''
        ''--add-flags dummett_library.core''
      ] ;

in {
  deps = build-deps ;
  dummett-library = pkgs.stdenv.mkDerivation {
    name = package-name ;
    version = package-version ;
    src = ./.. ;
    nativeBuildInputs = build-deps ;
    buildPhase =builtins.concatStringsSep "\n"
      (
        [
          ''export HOME="${deps-cache}"''
          ''export JAVA_TOOL_OPTIONS="-Duser.home=${deps-cache}"''
        ]
        ++ build-commands
      ) ;

    installPhase = builtins.concatStringsSep "\n"
      (install-commands ++ [wrapper-command]) ;
  } ;
}

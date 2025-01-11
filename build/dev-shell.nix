{pkgs, package-name, package-version}:
let
  utils = import ../build/utils.nix {pkgs = pkgs;} ;
  shell-fn = utils.shell-fn ;
  backend-build = [''clj -T:prod/build uber''] ;
  frontend-build = [''npm ci'' ''npx shadow-cljs release app''] ;
  build = utils.switch-fn {
    name = "build";
    switch-statements = {
      backend =  backend-build ;
      frontend = frontend-build ;
      all =  backend-build ++ frontend-build ;
    } ;
  };
  jar-file = "target/${package-name}-${package-version}-standalone.jar" ;
  backend-run = ''java -cp ${jar-file}  dummett_library.core'' ;
  frontend-run = "npx serve public" ;
  run = utils.switch-fn {
    name = "run" ;
    switch-statements = {
      backend = [ backend-run ];
      frontend = [ frontend-run ] ;
      all =
        [ # sed the outputs to append some cools stuff so we have
          # something coming through the stdout
          (backend-run + " & ")
          ''echo $! > run-pids.txt''
          (frontend-run + " & ")
          ''echo $! >> run-pids.txt''
        ] ;
    } ;
  } ;
  fns = builtins.concatStringsSep "\n" [build] ;
in pkgs.mkShell {
  INDEX_LOCATION="/opt/dummett";
  packages = with pkgs ; [ openjdk clojure nodejs git ] ;
  shellHook = fns + '' echo "go forth and search..."'';
}

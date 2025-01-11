{pkgs}:
let
  lib = pkgs.lib ;
  shell-fn = {name, commands}:
    "function " + name + " () {\n"
    + (builtins.foldl' (acc: command: acc + " " + command + "\n") "" commands)
    + "}\n" ;

  indent-fn = {indent , commands}:
    let
      padding = builtins.concatStringsSep "" (lib.replicate indent " ") ;
    in map (string: padding + string) commands ;


  switch-fn = {name, switch-statements}:
    let
      backend = indent-fn {indent = 8 ; commands = switch-statements.backend ; };
      frontend = indent-fn {indent = 8 ; commands = switch-statements.frontend ; };
    in
      (shell-fn {
        name = name;
        commands = (
          [''case $1 in''
           ''    backend)'']
          ++ backend
          ++ [ ''        ;;''
               ''    frontend)'']
          ++ frontend
          ++ [ ''        ;;''
               ''    all)'']
          ++ backend ++ frontend
          ++ [''        ;;''
              ''    *)''
              ''        echo "Unknown command $1: should be 'frontend' or 'backend'" ''
              ''        ;;''
              ''esac'']
        ); }) ;
in {
  shell-fn = shell-fn ;
  switch-fn = switch-fn;
}

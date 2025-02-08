{}:
let shell-fn = {name, commands}:
      let body = builtins.concatStringsSep "\n" commands ;
      in "function " + name + "() {\n" + body +  "\n}\n" ;
    pcu = "process-compose -f build/process-compose.yml -t=false up" ;
in {
  build = shell-fn {
    name = "build" ;
    commands = ["nix build .#all"];
  };
  clean = shell-fn {
    name = "clean" ;
    commands = ["rm -rf result"] ;
  } ;
  dev = shell-fn {
    name = "dev" ;
    commands = [
      ''case $1 in''
      ''    frontend)''
      ''        nix build .#backend''
      ''        ${pcu} backend/run frontend/dev''
      ''        ;;''
      ''    backend)''
      ''        ${pcu} backend/dev''
      ''        ;;''
      ''    *)''
      ''        echo "Cannot use $1: valid arguments are 'frontend' and 'backend'"''
      ''        ;;''
      ''esac''
    ];
  };
  run = shell-fn {
    name = "run";
    commands =  [
      "build"
      "${pcu} backend/run frontend/run"
    ] ;
  };
}

{description = "search the Dummett corpus" ;
 inputs = {
   nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable" ;
   flake-utils.url = "github.numtide/flake-utils";
 } ;
 outputs = {self, nixpkgs, flake-utils, ... }:
   flake-utils.lib.eachDefaultSystem (system:
     let pkgs = import nixpkgs { inherit system; };
         deps = [];
     in {
       # packages.default = pkgs.stdenv.mkDerivation {}
       devShells.default =
         let
           shell-fn = {name, commands}:
             "function " + name + " () {\n"
             + (builtins.foldl' (acc: elem: acc + "  " + elem + "\n") "" commands)
             + "}\n" ;
           fns = builtins.concatStringsSep "\n" [
             (shell-fn {
               name = "deps-lock" ;
               commands =
                 ["nix run github:jlesquembre/clj-nix#deps-lock"];
             })
           ] ;
         in
           pkgs.mkShell {
             packages = deps ;
             shellHook = fns + '' echo "go forth and search..."'';
           } ;
     }) ;
}

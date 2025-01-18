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
       packages.all = all-packages ;
       packages.default = frontend.dummett-library;});
}

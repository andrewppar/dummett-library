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
         dev = import ./build/dev.nix {};
         shell-fns = builtins.concatStringsSep "\n"
           [
             dev.build
             dev.clean
             dev.dev
             dev.run
           ];
     in {
       packages.backend = backend.dummett-library ;
       packages.frontend = frontend.dummett-library ;
       packages.all = all-packages ;
       packages.default = frontend.dummett-library;
       devShells.default = pkgs.mkShell {
         name = pname ;
         buildInputs = [ pkgs.process-compose ] ++ backend.deps ++ frontend.deps ;
         SECRET_KEY = "asdfSFS34wfsdfsdfSDSD32dfsddDDerQSNCK34SOWEK5354fdgdf4" ;
         shellHook =
           shell-fns + ''echo "Build the Dummett Library..."'';
       };
     });
}

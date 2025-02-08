{pkgs, package-name, package-version}:
let
  node-lock = builtins.fromJSON (builtins.readFile ../package-lock.json) ;
  node-deps = builtins.attrValues (removeAttrs node-lock.packages [""]) ;
  node-tarballs = map
    (p: pkgs.fetchurl { url = p.resolved; hash = p.integrity; })
    node-deps;
  node-tarballs-file = pkgs.writeTextFile {
    name = "tarballs";
    text = builtins.concatStringsSep "\n" node-tarballs;
  };
  build-dependencies = with pkgs ; [nodejs openjdk clojure git makeWrapper] ;
  makewrapper-command = builtins.concatStringsSep " "
    [
      ''makeWrapper''
      ''${pkgs.nodejs}/bin/npx''
      ''$out/bin/dummett_library_frontend''
      ''--add-flags http-server''
      ''--add-flags "$out/public"''
    ] ;
in {
  deps = build-dependencies ;
  dummett-library = pkgs.stdenv.mkDerivation {
    name = package-name ;
    version = package-version ;
    src = ./.. ;
    nativeBuildInputs = build-dependencies ;
    buildPhase = builtins.concatStringsSep "\n"
      [
        ''export HOME=$PWD/.home''
        ''export JAVA_TOOL_OPTIONS="-Duser.home=$HOME"''
        ''export npm_config_cache=$PWD/.npm''
        ''npm config set strict-ssl=false''

        ''while read package''
        ''do''
        ''  echo "caching $package"''
        ''  npm cache add "$package"''
        '' done <${node-tarballs-file}''
        ''npm ci''
        ''npx shadow-cljs release app''
      ] ;
    installPhase = builtins.concatStringsSep "\n"
      [
        ''mkdir -p $out/public''
        ''cp -r public $out/.''
        makewrapper-command
      ] ;
  } ;
}

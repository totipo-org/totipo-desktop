{ lib
, stdenv
, jdk
, gradle
, makeWrapper
, makeDesktopItem
, copyDesktopItems
, coreutils
, findutils
, gnused
}:
let
  rawVersion = builtins.readFile ./VERSION;
  version = if lib.hasSuffix "\r\n" rawVersion then lib.removeSuffix "\r\n" rawVersion else lib.removeSuffix "\n" rawVersion;
  # Only desktop sources, build inputs and packaged license notices.
  trees = [
    "src"
    "gradle"
    "packaging/licenses"
    "packaging/icons"
  ];
  files = [
    "build.gradle.kts"
    "settings.gradle.kts"
    "gradle.properties"
    "gradle.lockfile"
    "settings-gradle.lockfile"
    "VERSION"
    "LICENSE"
    "THIRD_PARTY.md"
  ];
in
assert builtins.match "[A-Za-z0-9][A-Za-z0-9._+-]*" version != null;
stdenv.mkDerivation (finalAttrs: {
  pname = "totipo-desktop";
  # Export the canonical ZIP from this build, never a second application build.
  outputs = [ "out" "portable" ];
  inherit version;
  src = lib.cleanSourceWith {
    src = ./.;
    filter = path: type:
      let
        rel = lib.removePrefix (toString ./. + "/") (toString path);
        parts = lib.splitString "/" rel;
        excluded = builtins.any (p: builtins.elem p [ ".git" ".gradle" "build" ".idea" ".vscode" ".direnv" "review" ]) parts
          || builtins.any (suffix: lib.hasSuffix suffix rel) [ "~" ".tmp" ".swp" ".iml" ];
        selected = builtins.elem rel files
          || builtins.any (tree: rel == tree || lib.hasPrefix (tree + "/") rel) trees
          || (type == "directory" && builtins.any (entry: lib.hasPrefix (rel + "/") entry) (trees ++ files));
      in
      !excluded && type != "symlink" && selected;
  };
  mitmCache = gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./package-deps.json;
  };
  nativeBuildInputs = [ gradle makeWrapper copyDesktopItems ];
  buildInputs = [ jdk ];
  JAVA_HOME = jdk;
  # NIO's filename encoding follows the native locale, not -Dfile.encoding.
  # Use glibc's built-in UTF-8 locale for both fetchDeps and package tests.
  LC_ALL = "C.UTF-8";
  gradleFlags = [
    "--no-configuration-cache"
    "--dependency-verification=strict"
  ];
  gradleBuildTask = "installDist verifyDistribution";
  doCheck = true;
  gradleCheckTask = "check";
  # Fetch exactly the tasks the package will execute, including desktop tests.
  gradleUpdateTask = "installDist verifyDistribution check";
  installPhase = ''
    runHook preInstall
    mkdir -p "$out/bin" "$out/lib/totipo-desktop" "$out/share/doc/totipo-desktop"
    cp -r build/install/totipo-desktop/{bin,lib} "$out/lib/totipo-desktop/"
    mv "$out/lib/totipo-desktop/bin/totipo-desktop" "$out/lib/totipo-desktop/bin/totipo-desktop-unwrapped"
    rm "$out/lib/totipo-desktop/bin/totipo-desktop.bat"
    cp -r build/install/totipo-desktop/{LICENSE,THIRD_PARTY.md,VERSION,licenses} "$out/share/doc/totipo-desktop/"
    makeWrapper "$out/lib/totipo-desktop/bin/totipo-desktop-unwrapped" "$out/bin/totipo-desktop" \
      --set JAVA_HOME ${jdk} \
      --prefix PATH : ${lib.makeBinPath [ coreutils findutils gnused ]}
    mkdir -p "$out/share/icons"
    cp -r packaging/icons/hicolor "$out/share/icons/"
    mkdir -p "$portable"
    cp build/distributions/totipo-desktop-${version}.zip "$portable/"
    cp build/reports/distribution/portable.sha256 "$portable/"
    runHook postInstall
  '';
  desktopItems = [
    (makeDesktopItem {
      name = "totipo-desktop";
      desktopName = "Totipo";
      icon = "totipo";
      exec = "totipo-desktop";
      categories = [ "Utility" ];
      terminal = false;
      comment = "Password-protected Totipo TOTP vaults";
    })
  ];
  doInstallCheck = true;
  installCheckPhase = ''
    runHook preInstallCheck
    test -x "$out/bin/totipo-desktop"
    grep -F -- "export JAVA_HOME='${jdk}'" "$out/bin/totipo-desktop"
    grep -F -- '-XX:+DisableAttachMechanism' "$out/lib/totipo-desktop/bin/totipo-desktop-unwrapped"
    test -x "$out/lib/totipo-desktop/bin/totipo-desktop-unwrapped"
    test -f "$out/lib/totipo-desktop/lib/totipo-desktop-${version}.jar"
    grep -F -- "$out/lib/totipo-desktop/bin/totipo-desktop-unwrapped" "$out/bin/totipo-desktop"
    test ! -e "$out/lib/totipo-desktop/bin/totipo-desktop.bat"
    # verifyDistribution checked final ZIP -> extracted -> original resolved JARs.
    # Compare actual installed hashes with that exact ZIP's per-file identities.
    sed -n 's@  totipo-desktop-${version}/lib/@  lib/@p' "$portable/portable.sha256" > payload.sha256
    test "$(wc -l < payload.sha256)" -eq 4
    test "$(find "$out/lib/totipo-desktop/lib" -type f | wc -l)" -eq 4
    payloadManifest="$PWD/payload.sha256"
    (cd "$out/lib/totipo-desktop"; sha256sum --check "$payloadManifest")
    (cd "$portable"; head -n 2 portable.sha256 | sha256sum --check)
    desktop="$out/share/applications/totipo-desktop.desktop"
    grep -x 'Icon=totipo' "$desktop"
    for icon in packaging/icons/hicolor/*/apps/totipo.*; do
      cmp "$icon" "$out/share/icons/hicolor/''${icon#packaging/icons/hicolor/}"
    done
    grep -x 'Name=Totipo' "$desktop"
    grep -x 'Exec=totipo-desktop' "$desktop"
    # makeDesktopItem joins lists without a trailing separator; both forms are valid.
    grep -Ex 'Categories=Utility;?' "$desktop"
    grep -x 'Terminal=false' "$desktop"
    for notice in LICENSE THIRD_PARTY.md VERSION; do
      cmp "build/install/totipo-desktop/$notice" "$out/share/doc/totipo-desktop/$notice"
    done
    for notice in BOUNCY_CASTLE_LICENSE.html TOTIPO_JAVA_LICENSE; do
      cmp "build/install/totipo-desktop/licenses/$notice" "$out/share/doc/totipo-desktop/licenses/$notice"
    done
    test -z "$(find "$out" \( -name '.gradle' -o -name '.git' -o -name '*.java' -o -iname '*junit*.jar' -o -iname '*test*.jar' \) -print -quit)"
    # Deployment-specific bytes after Nix fixups (wrapper, launcher, integration,
    # notices and JARs); distinct from the portable ZIP identity.
    (cd "$out"; find . -type f -print0 | sort -z | xargs -0 sha256sum) > "$portable/nix-deployment.sha256"
    runHook postInstallCheck
  '';
  meta = {
    description = "Swing desktop application for password-protected Totipo TOTP vaults";
    homepage = "https://github.com/totipo-org/totipo-desktop";
    license = lib.licenses.asl20;
    mainProgram = "totipo-desktop";
    platforms = lib.platforms.linux;
    sourceProvenance = with lib.sourceTypes; [ fromSource binaryBytecode ];
  };
})

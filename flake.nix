{
  description = "Development environment for totipo-desktop";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    llm-agents.url = "github:numtide/llm-agents.nix";
    jailed-agents = {
      url = "github:andersonjoseph/jailed-agents";
      inputs.llm-agents.follows = "llm-agents";
    };
  };

  outputs = { nixpkgs, flake-utils, jailed-agents, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
        };
        desktop = pkgs.callPackage ./package.nix {
          jdk = pkgs.jdk25;
          gradle = pkgs.gradle_9.override { java = pkgs.jdk25; };
        };
      in
      {
        formatter = pkgs.nixpkgs-fmt;

        packages = pkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isLinux {
          default = desktop;
        };

        # `nix flake check` is the normal qualification gate. checks.desktop
        # intentionally references the same derivation as packages.default.
        checks = pkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isLinux {
          desktop = desktop;
          wrapper = pkgs.runCommand "gradle-wrapper-integrity" { } ''
            echo '238e777fcddd7e34f9708186085def2abd6e08e658505b38718d79d74c21abd5  ${./gradle/wrapper/gradle-wrapper.jar}' | sha256sum --check -
            grep -Fx 'distributionSha256Sum=bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c' ${./gradle/wrapper/gradle-wrapper.properties}
            grep -Fx 'distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.0-bin.zip' ${./gradle/wrapper/gradle-wrapper.properties}
            touch "$out"
          '';
        };

        apps = pkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isLinux {
          update-package-deps = {
            type = "app";
            program = "${desktop.mitmCache.updateScript}";
            meta.description = "Update the Gradle dependency cache in package-deps.json";
          };
        };

        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            jdk25
            gradle_9

            (jailed-agents.lib.${system}.makeJailedCodex {
              fwdEnv = [ "JAVA_HOME" ];

              extraPkgs = with pkgs; [
                jdk25
                gradle_9
              ];
            })
          ];
        };
      });

  nixConfig = {
    extra-substituters = [ "https://cache.numtide.com" ];
    extra-trusted-public-keys = [ "niks3.numtide.com-1:DTx8wZduET09hRmMtKdQDxNNthLQETkc/yaX7M4qK0g=" ];
  };
}

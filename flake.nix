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
        jdk = pkgs.jdk25;
        gradle = pkgs.gradle_9.override { java = jdk; };
        desktop = pkgs.callPackage ./package.nix { inherit jdk gradle; };
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
        };

        apps = pkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isLinux {
          update-package-deps = {
            type = "app";
            program = "${desktop.mitmCache.updateScript}";
            meta.description = "Update the Gradle dependency cache in package-deps.json";
          };
        };

        devShells.default = pkgs.mkShell {
          JAVA_HOME = "${jdk}/lib/openjdk";
          packages = [
            jdk
            gradle
            pkgs.coreutils

            (jailed-agents.lib.${system}.makeJailedCodex {
              fwdEnv = [ "JAVA_HOME" ];

              extraPkgs = [
                jdk
                gradle
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

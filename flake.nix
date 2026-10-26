{
  description = "Development environment for totipo-desktop";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    java-dev-tools = {
      url = "github:ingon/java-dev-tools";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    llm-agents.url = "github:numtide/llm-agents.nix";
    jailed-agents = {
      url = "github:andersonjoseph/jailed-agents";
      inputs.llm-agents.follows = "llm-agents";
    };
  };

  outputs = { self, nixpkgs, flake-utils, jailed-agents, java-dev-tools, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
        };
        jdk = pkgs.jdk25;
        gradle = pkgs.gradle_9.override { java = jdk; };
        javaEditor = java-dev-tools.lib.mkJavaEditor { inherit pkgs jdk gradle; };
        desktop = pkgs.callPackage ./package.nix { inherit jdk gradle; };
        releaseSourceIdentity = pkgs.writeText "totipo-release-source.json" (builtins.toJSON {
          revision = self.rev or null;
          dirtyRevision = self.dirtyRev or null;
          collectorSha256 = builtins.hashFile "sha256" ./qualification/collect-release-evidence.sh;
          inherit system;
          version = desktop.version;
        });
        collectReleaseEvidence = pkgs.writeShellApplication {
          name = "collect-release-evidence";
          runtimeInputs = [ pkgs.coreutils pkgs.findutils pkgs.gnused pkgs.gnugrep pkgs.diffutils pkgs.unzip pkgs.nix ];
          text = ''
            exec ${pkgs.bash}/bin/bash ${./qualification/collect-release-evidence.sh} \
              ${desktop} ${desktop.portable} ${jdk} ${releaseSourceIdentity} "$@"
          '';
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
        };

        apps = pkgs.lib.optionalAttrs pkgs.stdenv.hostPlatform.isLinux {
          collect-release-evidence = {
            type = "app";
            program = "${collectReleaseEvidence}/bin/collect-release-evidence";
            meta.description = "Record and verify release output identities without GUI qualification";
          };
          update-package-deps = {
            type = "app";
            program = "${desktop.mitmCache.updateScript}";
            meta.description = "Update the Gradle dependency cache in package-deps.json";
          };
        };

        devShells.default = pkgs.mkShell (javaEditor.shellEnv // {
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
          ] ++ javaEditor.packages;
        });
      });

  nixConfig = {
    extra-substituters = [ "https://cache.numtide.com" ];
    extra-trusted-public-keys = [ "niks3.numtide.com-1:DTx8wZduET09hRmMtKdQDxNNthLQETkc/yaX7M4qK0g=" ];
  };
}

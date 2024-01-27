{ pkgs ? import <nixpkgs> {} }:

let
  pname = "kicks";
in
  pkgs.mkShell {
    nativeBuildInputs = with pkgs; [
      git

      nodejs-16_x
      yarn

      sbt
    ];

    shellHook = with pkgs; ''
      echo --- Welcome to ${pname}! ---
    '';
  }

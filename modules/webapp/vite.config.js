import { defineConfig } from "vite";
// import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import path from 'path';
import fs from 'fs';

const isProd = process.env.NODE_ENV == "production";

//TODO: https://github.com/scala-js/vite-plugin-scalajs/issues/19
// process.env["SBT_NATIVE_CLIENT"]="false";

export default defineConfig({
  // plugins: [
  //   scalaJSPlugin({
  //     cwd: '../../',
  //     projectID: 'webapp',
  //     uriPrefix: 'scalajs',
  //   }),
  // ],
  resolve: {
    alias: [
      { find: '@', replacement: path.resolve(`./target/scalajs-${isProd ? "opt" : "fastopt"}`) },
    ],
  },
  server: {
    proxy: {
      '/Rpc/': 'http://localhost:8080',
      '/EventRpc/': 'http://localhost:8080',
      '/api/': 'http://localhost:8080',
      '/info/': 'http://localhost:8080',
    }
  },
});

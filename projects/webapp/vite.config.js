import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

//TODO: https://github.com/scala-js/vite-plugin-scalajs/issues/19
process.env["SBT_NATIVE_CLIENT"]="false";

export default defineConfig({
  plugins: [
    scalaJSPlugin({
      cwd: '../../',
      projectID: 'webapp',
      uriPrefix: 'scalajs',
    }),
  ],
});

module.exports = {
  content: ["index.html", "src/**/*.scala"],
  plugins: [
    require('daisyui'),
  ],
  daisyui: {
    themes: [
      'light',
      'dark'
    ]
  },
};

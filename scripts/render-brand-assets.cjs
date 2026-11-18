const fs = require('node:fs');
const path = require('node:path');
const { Resvg } = require('@resvg/resvg-js');
const folder = path.resolve(__dirname, '../assets/brand');
for (const name of fs.readdirSync(folder).filter(name => name.endsWith('.svg'))) {
  const width = name.includes('preview') ? 1600 : name.includes('app-icon') ? 1024 : name.includes('wordmark') ? 1544 : 512;
  const renderer = new Resvg(fs.readFileSync(path.join(folder, name)), {
    fitTo: { mode: 'width', value: width },
    font: { loadSystemFonts: true, defaultFontFamily: 'Arial' },
  });
  fs.writeFileSync(path.join(folder, name.replace('.svg', '.png')), renderer.render().asPng());
}
console.log('Rendered transparent wordmarks, symbols, app icon and brand preview.');

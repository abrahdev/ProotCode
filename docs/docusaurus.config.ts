import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import {themes as prismThemes} from 'prism-react-renderer';

const config: Config = {
  title: 'ProotCode',
  tagline: 'A portable Linux development workspace for Android',
  favicon: 'img/logo.svg',

  url: 'https://abrahdev.github.io',
  baseUrl: '/ProotCode/',
  organizationName: 'abrahdev',
  projectName: 'ProotCode',
  trailingSlash: false,

  onBrokenLinks: 'throw',

  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/abrahdev/ProotCode/edit/main/docs/',
          routeBasePath: '/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themes: ['@docusaurus/theme-mermaid'],

  themeConfig: {
    image: 'img/logo.svg',
    navbar: {
      title: 'ProotCode',
      logo: {
        alt: 'ProotCode logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'mainSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          href: 'https://github.com/abrahdev/ProotCode',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {label: 'Installation', to: '/installation'},
            {label: 'Architecture', to: '/architecture'},
            {label: 'Roadmap', to: '/roadmap'},
          ],
        },
        {
          title: 'Community',
          items: [
            {label: 'GitHub', href: 'https://github.com/abrahdev/ProotCode'},
            {label: 'Issues', href: 'https://github.com/abrahdev/ProotCode/issues'},
          ],
        },
        {
          title: 'More',
          items: [
            {label: 'Contributing', to: '/contributing'},
            {label: 'License', href: 'https://github.com/abrahdev/ProotCode/blob/main/LICENSE'},
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} ProotCode contributors. AGPL-3.0.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;

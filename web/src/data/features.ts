export interface Feature {
  num: string
  title: string
  desc: string
  hot?: boolean
  link?: { text: string; href: string }
}

export const features: Feature[] = [
  { num: '01', title: 'Distro selector', desc: 'Pick Debian or Ubuntu for each new environment.', hot: true },
  { num: '02', title: 'Rootless Linux', desc: 'proot-based install. No root access, ever.' },
  { num: '03', title: 'Non-root user', desc: 'A regular user with a persistent home directory.' },
  { num: '04', title: 'Storage bridge', desc: 'Bind Android folders like Downloads into your Linux home.' },
  { num: '05', title: 'Package manager', desc: 'Run apt commands straight from the app interface.' },
  { num: '06', title: 'Essential presets', desc: 'Git, build-essential, curl and OpenCode pre-installed.' },
  { num: '07', title: 'Tool selection', desc: 'Choose dev tools by category before installing.' },
  { num: '08', title: 'Multi terminals', desc: 'Several sessions open on the same distribution.', hot: true },
  { num: '09', title: 'Code Server', desc: 'VS Code in the browser, on your local network, with mobile CSS.', hot: true },
  { num: '10', title: 'VR Support', desc: 'Run ProotCode from the Meta Quest 3 browser.', link: { text: 'View more', href: '#vr' } },
  { num: '11', title: 'Resource controls', desc: 'Monitor and limit battery and CPU usage.' },
  { num: '12', title: 'Install progress', desc: 'Follow every step of the distribution setup.' },
  { num: '13', title: 'Backups', desc: 'Export and restore projects and configuration.' },
]

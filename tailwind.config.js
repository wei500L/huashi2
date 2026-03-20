import tailwindcssAnimate from 'tailwindcss-animate';

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: ["class"],
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "var(--background)",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        glass: {
          light: "rgba(255, 255, 255, 0.03)",
          DEFAULT: "rgba(255, 255, 255, 0.06)",
          dark: "rgba(0, 0, 0, 0.2)",
          border: "rgba(255, 255, 255, 0.08)",
        }
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
        '4xl': "2rem",
        '5xl': "3rem",
      },
      backgroundImage: {
        'glass-gradient': 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, rgba(255,255,255,0.01) 100%)',
        'noise': "url('https://grainy-gradients.vercel.app/noise.svg')",
      },
      animation: {
        'fluid-flow': 'flow 15s ease-in-out infinite',
        'float': 'float 20s ease-in-out infinite',
        'float-delayed': 'float 25s ease-in-out infinite 2s',
        'pulse-slow': 'pulse 8s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'border-beam': 'border-beam 4s ease infinite',
        'page-reveal': 'page-reveal 1.2s cubic-bezier(0.77, 0, 0.175, 1) forwards',
      },
      keyframes: {
        flow: {
          '0%, 100%': { backgroundPosition: '0% 50%' },
          '50%': { backgroundPosition: '100% 50%' },
        },
        float: {
          '0%, 100%': { transform: 'translate(0, 0) scale(1)' },
          '33%': { transform: 'translate(30px, -50px) scale(1.1)' },
          '66%': { transform: 'translate(-20px, 20px) scale(0.9)' },
        },
        'border-beam': {
          '100%': { 'offset-distance': '100%' },
        },
        reveal: {
          '0%': { clipPath: 'polygon(0 100%, 100% 100%, 100% 100%, 0% 100%)', transform: 'translateY(40px)' },
          '100%': { clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)', transform: 'translateY(0)' },
        },
        'page-reveal': {
          '0%': { transform: 'translateY(0)' },
          '100%': { transform: 'translateY(-100%)' },
        },
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        }
      },
      transitionTimingFunction: {
        'expo-out': 'cubic-bezier(0.16, 1, 0.3, 1)',
        'expo-in-out': 'cubic-bezier(0.87, 0, 0.13, 1)',
        'fluid': 'cubic-bezier(0.23, 1, 0.32, 1)',
      }
    },
  },
  plugins: [tailwindcssAnimate],
}

export const optionsGraph: any = {
  fill: true,
  radius: 0.5,
  responsive: true,
  aspectRatio: 1.6,
  interaction: {
    mode: 'index',
    intersect: false
  },
  plugins: {
    title: {
      display: false
    },
    legend: {
      align: 'center',
      position: 'bottom',
      labels: {
        boxWidth: 16,
        borderRadius: 20,
        useBorderRadius: true,
        padding: 20,
        font: {
          size: 15
        },
        usePointStyle: true
      }
    },
    tooltip: {
      usePointStyle: true,
      callbacks: {
        label: (context: any) => {
          return ` ${context.label} - ${context.parsed.y.toFixed(1)} Gb`
        },
        title: () => {
          return ''
        },
        labelPointStyle: () => {
          return {
            pointStyle: 'circle',
            rotation: 0
          }
        }
      }
    }
  },
  scales: {
    x: {
      grid: {
        display: false
      },
      ticks: {
        maxRotation: 0,
        font: {
          size: 10
        }
      }
    },
    y: {
      display: true,
      position: 'right',
      border: {
        display: true
      },
      grid: {
        display: false
      },
      ticks: {
        callback(val: number | string): string {
          return val + ' Gb'
        }
      }
    }
  }
}

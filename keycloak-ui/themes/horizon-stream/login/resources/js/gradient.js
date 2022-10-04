let interval1
let interval2
let interval3
let interval4

const animateGradient = () => {
  const theme = window.localStorage.getItem('theme')
  const isDark = theme === 'dark'

  // set backdrop gradient
  const dark = [10, 12, 27, 1]
  const light = [255, 255, 255, 0]
  const backdrop = isDark ? dark : light

  // set gradient color spectrum
  const rgbs = [[36, 49, 132, 0.3], [251, 135, 191, 0.3], [20, 209, 233, 0.3], backdrop]

  const randum = (max, min = 0) => Math.round(Math.random() * (max - min) + min)

  const getRbga = (use) => {
    const rgba = use || rgbs[randum(3)]
    return `rgba(${rgba[0]}, ${rgba[1]}, ${rgba[2]}, ${rgba[3]})`
  }

  const blender = () => {
    const p1 = randum(100, 50)
    const p2 = randum(100, 50)
    const c1 = getRbga(backdrop)
    const c2 = getRbga()
    const c4 = getRbga(backdrop)
    const c5 = getRbga()
    const c6 = getRbga(backdrop)

    return {
      blend: `radial-gradient(circle at ${p1}% ${p2}%, ${c1}, ${c2}, ${c4}, ${c5}, ${c6})`,
    }
  }

  const createStyles = (style, index, seconds) => {
    const b = blender()
    style.innerHTML = `.quad${index} {background: transparent}`
    style.innerHTML += `.quad${index}::after {content: ""}`
    style.innerHTML += `.quad${index}::after {position: absolute}`
    style.innerHTML += `.quad${index}::after {top: 0}`
    style.innerHTML += `.quad${index}::after {left: 0}`
    style.innerHTML += `.quad${index}::after {right: 0}`
    style.innerHTML += `.quad${index}::after {bottom: 0}`
    style.innerHTML += `.quad${index}::after {opacity: 0}`
    style.innerHTML += `.quad${index}::after {animation: gradient ${seconds}s alternate infinite}`
    style.innerHTML += `.quad${index}::after {background: ${b.blend}}`
  }

  const html = document.querySelector('html')
  const style1 = document.createElement('style')
  const style2 = document.createElement('style')
  const style3 = document.createElement('style')
  const style4 = document.createElement('style')
  const styles = [style1, style2, style3, style4]

  const topLeft = document.createElement('div')
  const topRight = document.createElement('div')
  const bottomLeft = document.createElement('div')
  const bottomRight = document.createElement('div')
  const divs = [topLeft, topRight, bottomLeft, bottomRight]

  // position each quadrant
  topLeft.style.top = '0px'
  topRight.style.top = '0px'
  bottomLeft.style.bottom = '0px'
  bottomRight.style.bottom = '0px'
  topLeft.style.left = '0px'
  topRight.style.right = '0px'
  bottomLeft.style.left = '0px'
  bottomRight.style.right = '0px'

  // add default styles
  for (const [index, div] of divs.entries()) {
    div.style.background = 'transparent'
    div.style.position = 'fixed'
    div.style.width = '100%'
    div.style.height = '100%'
    div.className = 'quad' + index

    // add each quadrant to html
    if (html) html.appendChild(div)
  }

  if (html) {
    // add style for each quad
    for (const style of styles) html.appendChild(style)
  }

  // init gradient
  createStyles(style1, 0, 4)
  createStyles(style2, 1, 5)
  createStyles(style3, 2, 6)
  createStyles(style4, 3, 7)
}

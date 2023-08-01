<template>
  <svg :width="windowWidth" :height="windowHeight" :viewBox="`0 0 ${windowWidth} ${windowHeight}`" fill="none"
    xmlns="http://www.w3.org/2000/svg" data-test="gradient-bg">
    <g clip-path="url(#gradient-bg-clipping-path)">
      <g opacity="0.7" style="filter:url(#blur);">
        <path d="M507.711 1026.44L-14 1259H1511V634H876.921C716.395 634 563.895 895.628 507.711 1026.44Z"
          fill="#FF555E" />
        <ellipse opacity="0.5" cx="1068" cy="869" rx="424" ry="423" fill="#14D1DF" />
        <ellipse opacity="0.5" cx="1328" cy="545.5" rx="146" ry="213.5" fill="#131736" />
      </g>
      <g opacity="0.7" style="filter:url(#blur2);mix-blend-mode:overlay;">
        <ellipse cx="330.992" cy="345.595" rx="330.992" ry="345.595"
          transform="matrix(0.9957 -0.0926363 0.0350879 0.999384 438.711 568.249)" fill="#0081AD" />
        <ellipse cx="399.125" cy="252.537" rx="399.125" ry="252.537"
          transform="matrix(0.999999 0.00132984 0.0704685 0.997514 806.184 806)" fill="#7289FF" />
        <ellipse cx="1485.65" cy="1077.87" rx="154.432" ry="308.865" fill="#14D1DF" />
        <ellipse cx="480.774" cy="987" rx="608.774" ry="218" fill="#273180" />
      </g>
    </g>
    <defs>
      <filter id="blur" x="-814" y="-468" width="3125" height="2560" filterUnits="userSpaceOnUse"
        color-interpolation-filters="sRGB">
        <feFlood flood-opacity="0" />
        <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" />
        <feGaussianBlur stdDeviation="400" />
      </filter>
      <filter id="blur2" x="-828" y="-163.793" width="3168.08" height="2250.52" filterUnits="userSpaceOnUse"
        color-interpolation-filters="sRGB">
        <feFlood flood-opacity="0" />
        <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" />
        <feGaussianBlur stdDeviation="350" />
      </filter>
      <clipPath id="gradient-bg-clipping-path">
        <rect :width="windowWidth" :height="windowHeight" fill="white" />
      </clipPath>
    </defs>
  </svg>
</template>
<script lang="ts" setup>
const windowWidth = ref(window.innerWidth)
const windowHeight = ref(window.innerHeight)

const updateSize = () => {
  windowWidth.value = window.innerWidth
  windowHeight.value = window.innerHeight
}

onMounted(() => {
  window.addEventListener('resize', updateSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateSize)
})
</script>
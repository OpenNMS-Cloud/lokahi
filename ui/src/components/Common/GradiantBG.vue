<template>
  <svg :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" fill="none" xmlns="http://www.w3.org/2000/svg"
    data-test="gradiant-bg">
    <g clip-path="url(#clip0_2548_16987)">
      <g opacity="0.7" filter="url(#filter0_f_2548_16987)">
        <path d="M507.711 1026.44L-14 1259H1511V634H876.921C716.395 634 563.895 895.628 507.711 1026.44Z"
          fill="#FB9AC5" />
        <ellipse opacity="0.5" cx="1068" cy="869" rx="424" ry="423" fill="#14D1DF" />
        <ellipse opacity="0.5" cx="1328" cy="545.5" rx="146" ry="213.5" fill="#131736" />
      </g>
      <g style="mix-blend-mode: overlay" opacity="0.7" filter="url(#filter1_f_2548_16987)">
        <ellipse cx="330.992" cy="345.595" rx="330.992" ry="345.595"
          transform="matrix(0.9957 -0.0926363 0.0350879 0.999384 438.711 568.249)" fill="#697CFF" />
        <ellipse cx="399.125" cy="252.537" rx="399.125" ry="252.537"
          transform="matrix(0.999999 0.00132984 0.0704685 0.997514 806.184 806)" fill="#3A4BD3" />
        <ellipse cx="1485.65" cy="1077.87" rx="154.432" ry="308.865" fill="#00BFCB" />
        <ellipse cx="480.774" cy="987" rx="608.774" ry="218" fill="#273180" />
      </g>
    </g>
    <defs>
      <filter id="filter0_f_2548_16987" x="-814" y="-468" width="3125" height="2560" filterUnits="userSpaceOnUse"
        color-interpolation-filters="sRGB">
        <feFlood flood-opacity="0" result="BackgroundImageFix" />
        <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape" />
        <feGaussianBlur stdDeviation="400" result="effect1_foregroundBlur_2548_16987" />
      </filter>
      <filter id="filter1_f_2548_16987" x="-828" y="-163.793" width="3168.08" height="2250.52"
        filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
        <feFlood flood-opacity="0" result="BackgroundImageFix" />
        <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape" />
        <feGaussianBlur stdDeviation="350" result="effect1_foregroundBlur_2548_16987" />
      </filter>
      <clipPath id="clip0_2548_16987">
        <rect :width="width" :height="height" fill="white" />
      </clipPath>
    </defs>
  </svg>
</template>
<script lang="ts" setup>
const width = window.innerWidth
const height = window.innerHeight
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
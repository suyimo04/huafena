import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// Install navigation guard after Pinia is ready
setupRouterGuard()

app.mount('#app')

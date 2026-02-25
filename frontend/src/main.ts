import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// Install navigation guard after Pinia is ready
setupRouterGuard()

app.mount('#app')

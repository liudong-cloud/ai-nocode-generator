/**
 * 可视化编辑器 Composable
 *
 * 通过直接操作同域 iframe 的 contentDocument，实现：
 *  - 鼠标悬浮高亮
 *  - 点击选中元素，获取元素信息
 *  - 进入/退出编辑模式
 */
import { ref, onUnmounted, type Ref } from 'vue'

/** 选中元素的信息结构 */
export interface SelectedElementInfo {
  /** 标签名，如 h1 / p / button */
  tagName: string
  /** id 属性 */
  id: string
  /** class 属性 */
  className: string
  /** 截取的文本内容（最多 100 字符） */
  textContent: string
  /** CSS 选择器路径 */
  selector: string
  /** 人类可读的简短描述 */
  description: string
  /** iframe 内页面路径（pathname） */
  pagePath?: string
}

const STYLE_ELEMENT_ID = 'visual-editor-injected-style'

/** 为元素生成一个简单的 CSS 选择器路径 */
function buildSelector(el: HTMLElement): string {
  if (el.id) return `#${el.id}`
  const parts: string[] = []
  let current: HTMLElement | null = el
  while (current && current.tagName.toLowerCase() !== 'body') {
    let part = current.tagName.toLowerCase()
    if (current.id) {
      part += `#${current.id}`
      parts.unshift(part)
      break
    }
    if (current.className && typeof current.className === 'string') {
      const classes = current.className.trim().split(/\s+/).join('.')
      if (classes) part += `.${classes}`
    }
    parts.unshift(part)
    current = current.parentElement
  }
  return parts.join(' > ') || el.tagName.toLowerCase()
}

/** 生成人类可读的简短描述 */
function buildDescription(el: HTMLElement): string {
  const tag = el.tagName.toLowerCase()
  const id = el.id ? `#${el.id}` : ''
  const text = el.textContent?.trim().slice(0, 40) || ''
  const textPart = text ? ` "${text}"` : ''
  return `<${tag}${id}>${textPart}`
}

export function useVisualEditor(iframeRef: Ref<HTMLIFrameElement | null | undefined>) {
  const isEditMode = ref(false)
  const selectedElement = ref<SelectedElementInfo | null>(null)

  // 记录当前 hover/selected 元素，用于清理旧属性
  let hoveredEl: HTMLElement | null = null
  let selectedEl: HTMLElement | null = null

  // ─── 事件处理器（在 iframe 的 document 上注册）────────────────────────────

  const handleMouseOver = (e: Event) => {
    const el = e.target as HTMLElement
    if (el === document.body || el === hoveredEl) return
    hoveredEl?.removeAttribute('data-ve-hover')
    hoveredEl = el
    el.setAttribute('data-ve-hover', '')
  }

  const handleMouseOut = (e: Event) => {
    const el = e.target as HTMLElement
    el.removeAttribute('data-ve-hover')
    if (hoveredEl === el) hoveredEl = null
  }

  const handleClick = (e: Event) => {
    e.preventDefault()
    e.stopPropagation()

    const el = e.target as HTMLElement
    // 清除旧选中状态
    selectedEl?.removeAttribute('data-ve-selected')
    selectedEl = el
    el.setAttribute('data-ve-selected', '')

    selectedElement.value = {
      tagName: el.tagName.toLowerCase(),
      id: el.id || '',
      className: typeof el.className === 'string' ? el.className : '',
      textContent: el.textContent?.trim().slice(0, 100) || '',
      selector: buildSelector(el),
      description: buildDescription(el),
      pagePath: iframeRef.value?.contentWindow?.location.pathname || undefined,
    }
  }

  // ─── 样式注入 / 清理 ──────────────────────────────────────────────────────

  const injectStyle = (doc: Document) => {
    if (doc.getElementById(STYLE_ELEMENT_ID)) return
    const style = doc.createElement('style')
    style.id = STYLE_ELEMENT_ID
    style.textContent = `
      [data-ve-hover] {
        outline: 2px dashed #38b2ac !important;
        outline-offset: 2px !important;
        cursor: pointer !important;
      }
      [data-ve-selected] {
        outline: 2px solid #4299e1 !important;
        outline-offset: 2px !important;
        background-color: rgba(66, 153, 225, 0.06) !important;
      }
    `
    doc.head.appendChild(style)
  }

  const removeStyle = (doc: Document) => {
    doc.getElementById(STYLE_ELEMENT_ID)?.remove()
  }

  // ─── 进入 / 退出编辑模式 ─────────────────────────────────────────────────

  const getDoc = (): Document | null => iframeRef.value?.contentDocument ?? null

  const attachListeners = (doc: Document) => {
    doc.body.addEventListener('mouseover', handleMouseOver)
    doc.body.addEventListener('mouseout', handleMouseOut)
    doc.body.addEventListener('click', handleClick, true)
  }

  const detachListeners = (doc: Document) => {
    doc.body.removeEventListener('mouseover', handleMouseOver)
    doc.body.removeEventListener('mouseout', handleMouseOut)
    doc.body.removeEventListener('click', handleClick, true)
  }

  const enterEditMode = () => {
    const doc = getDoc()
    if (!doc) return
    isEditMode.value = true
    injectStyle(doc)
    attachListeners(doc)
  }

  const exitEditMode = () => {
    isEditMode.value = false
    clearSelection()
    const doc = getDoc()
    if (!doc) return
    removeStyle(doc)
    detachListeners(doc)
    hoveredEl = null
  }

  const toggleEditMode = () => {
    if (isEditMode.value) exitEditMode()
    else enterEditMode()
  }

  /** 仅清除选中状态，保持编辑模式 */
  const clearSelection = () => {
    selectedElement.value = null
    selectedEl?.removeAttribute('data-ve-selected')
    selectedEl = null
  }

  /**
   * 绑定到 iframe 的 @load 事件。
   * 当 iframe 内容（例如预览刷新后）重新加载时，若还在编辑模式则重新注入。
   */
  const handleIframeLoad = () => {
    if (!isEditMode.value) return
    // 等待 document 渲染完毕
    setTimeout(() => {
      const doc = getDoc()
      if (!doc) return
      injectStyle(doc)
      attachListeners(doc)
    }, 150)
  }

  onUnmounted(() => {
    exitEditMode()
  })

  return {
    isEditMode,
    selectedElement,
    toggleEditMode,
    enterEditMode,
    exitEditMode,
    clearSelection,
    handleIframeLoad,
  }
}

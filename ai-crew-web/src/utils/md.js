import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true, gfm: true })

/** 渲染 markdown 并消毒（模型输出的代码块/列表等） */
export function renderMd(text) {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(String(text)))
}

/** 后端 ISO 时间转本地可读 */
export function fmtTime(s) {
  if (!s) return ''
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

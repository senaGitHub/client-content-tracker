/**
 * The bundled brand covers in frontend/public/images/covers. Mirrors the backend's CoverImages
 * list (the API rejects any other path), so a new photo has to be added in both places.
 */
export const COVER_IMAGES: { url: string; label: string }[] = [
  { url: '/images/covers/infinity-pool.jpg', label: 'Sonsuzluk havuzu' },
  { url: '/images/covers/cappadocia-balloon.jpg', label: 'Kapadokya balonları' },
  { url: '/images/covers/palm-pool.jpg', label: 'Palmiyeli havuz' },
  { url: '/images/covers/spa-pool.jpg', label: 'Spa havuzu' },
  { url: '/images/covers/antalya-cliffs.jpg', label: 'Antalya kayalıkları' },
  { url: '/images/covers/seaside-yoga.jpg', label: 'Deniz kenarında yoga' },
  { url: '/images/covers/olive-table.jpg', label: 'Zeytin sofrası' },
]

/** Up to two initials for the no-cover fallback: "Olive & Stone Spa" → "OS", "Medina" → "ME". */
export function brandInitials(name: string): string {
  const words = name
    .trim()
    .split(/\s+/)
    .filter((word) => /\p{L}|\p{N}/u.test(word[0] ?? ''))
  if (words.length === 0) return ''
  if (words.length === 1) return words[0]!.slice(0, 2).toLocaleUpperCase('tr')
  return (words[0]![0]! + words[1]![0]!).toLocaleUpperCase('tr')
}

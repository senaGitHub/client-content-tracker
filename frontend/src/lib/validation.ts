/*
 * Text rules for form fields, and their Turkish messages: the one frontend place for both.
 *
 * They mirror the backend's Bean Validation on ClientRequest and ContentRequest (@Size min and
 * @Pattern with \p{L}), so the form says the same thing before sending as the server would after.
 * lib/errors.ts uses the same message builders to translate the server's English messages.
 * If a rule changes, change it here and in the matching Java record.
 */

export interface TextRule {
  /** Minimum length after trimming. */
  min?: number
  /** At least one letter (any script, so Turkish letters count). */
  needsLetter?: boolean
  /** Letters and spaces only, plus these characters (shown to the user as written). */
  allowedChars?: string
}

export const TEXT_RULES: Record<string, TextRule> = {
  // Client
  name: { min: 2, needsLetter: true },
  industry: { min: 2, allowedChars: '& - , .' },
  contactName: { min: 2, allowedChars: "' - ." },
  // Content
  title: { needsLetter: true },
}

const LETTER = /\p{L}/u

function onlyAllowed(value: string, chars: string): boolean {
  const extra = chars.replace(/\s/g, '').replace(/[\\\]^-]/g, '\\$&')
  return new RegExp(`^[\\p{L} ${extra}]+$`, 'u').test(value)
}

/** Turkish messages, by rule. `label` is the field's Turkish name, e.g. "Marka adı". */
export const messages = {
  minLength: (label: string, n: number) => `${label} en az ${n} karakter olmalı.`,
  needsLetter: (label: string) => `${label} en az bir harf içermeli.`,
  onlyLetters: (label: string, chars?: string) =>
    chars ? `${label} yalnızca harf, boşluk ve ${chars} içerebilir.` : `${label} yalnızca harf ve boşluk içerebilir.`,
}

/**
 * The first broken rule for a non-empty value, as a Turkish message; undefined when it passes
 * (or the field has no rules). Empty values are left to each form's "required" check.
 */
export function checkText(field: string, label: string, raw: string): string | undefined {
  const rule = TEXT_RULES[field]
  const value = raw.trim()
  if (!rule || !value) return undefined
  if (rule.min && value.length < rule.min) return messages.minLength(label, rule.min)
  if (rule.needsLetter && !LETTER.test(value)) return messages.needsLetter(label)
  if (rule.allowedChars !== undefined && !onlyAllowed(value, rule.allowedChars)) {
    return messages.onlyLetters(label, rule.allowedChars)
  }
  return undefined
}

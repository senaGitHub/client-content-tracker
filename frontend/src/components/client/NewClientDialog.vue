<script setup lang="ts">
import { CheckIcon, XIcon } from '@lucide/vue'
import { nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { ApiError, clientsApi } from '@/api'
import type { ClientRequest } from '@/api'
import BrandCover from '@/components/client/BrandCover.vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { notifyClientSaved, useClientDialog } from '@/composables/useClientDialog'
import { useClientOptions } from '@/composables/useClientOptions'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { clientLink } from '@/lib/content'
import { COVER_IMAGES } from '@/lib/covers'
import { errorMessage, fieldErrors } from '@/lib/errors'
import { cn } from '@/lib/utils'

type Field = 'name' | 'industry' | 'contactName' | 'contactEmail' | 'coverImageUrl'
// Order matters: the first invalid field in this order gets focus.
const FIELDS: Field[] = ['name', 'industry', 'contactName', 'contactEmail', 'coverImageUrl']
// A light check only; the server's @Email has the final say.
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const { state } = useClientDialog()
const { reload: reloadClientOptions } = useClientOptions()
const router = useRouter()

/** '' = no cover (the initials fallback). */
const form = reactive({ name: '', industry: '', contactName: '', contactEmail: '', coverImageUrl: '' })
const errors = reactive<Partial<Record<Field, string>>>({})
const saving = ref(false)

function clearErrors() {
  FIELDS.forEach((f) => delete errors[f])
}

watch(() => state.open, (open) => {
  if (!open) return
  Object.assign(form, { name: '', industry: '', contactName: '', contactEmail: '', coverImageUrl: '' })
  clearErrors()
})

function validate(): boolean {
  clearErrors()
  if (!form.name.trim()) errors.name = 'Marka adı gerekli.'
  if (!form.industry.trim()) errors.industry = 'Sektör gerekli.'
  if (!form.contactName.trim()) errors.contactName = 'İlgili kişi gerekli.'
  if (!form.contactEmail.trim()) errors.contactEmail = 'E-posta gerekli.'
  else if (!EMAIL.test(form.contactEmail.trim())) errors.contactEmail = 'Geçerli bir e-posta adresi girin.'
  return Object.keys(errors).length === 0
}

const id = (field: Field) => `new-client-${field}`
const describedBy = (field: Field) => (errors[field] ? `${id(field)}-error` : undefined)

async function focusFirstError() {
  await nextTick()
  const field = FIELDS.find((f) => errors[f])
  if (!field) return
  const el = document.getElementById(id(field))
  const target = el?.matches('input') ? el : el?.querySelector<HTMLElement>('input:checked, input')
  target?.focus()
}

function body(): ClientRequest {
  return {
    name: form.name.trim(),
    industry: form.industry.trim(),
    contactName: form.contactName.trim(),
    contactEmail: form.contactEmail.trim(),
    coverImageUrl: form.coverImageUrl || null,
  }
}

async function submit() {
  if (saving.value) return
  if (!validate()) return focusFirstError()
  saving.value = true
  try {
    const saved = await clientsApi.create(body())
    toast.success('Marka eklendi', {
      description: saved.name,
      action: { label: 'Markayı aç', onClick: () => void router.push(clientLink(saved.id)) },
    })
    notifyClientSaved(saved)
    void refreshNavCounts()
    void reloadClientOptions()
    state.open = false
  } catch (e) {
    if (e instanceof ApiError && e.status === 400 && Object.keys(e.fieldErrors).length) {
      Object.assign(errors, fieldErrors(e))
      void focusFirstError()
    } else {
      toast.error('Marka kaydedilemedi', { description: errorMessage(e) })
    }
  } finally {
    saving.value = false
  }
}

const labelClass = 'text-sm font-semibold'
const optional = 'font-normal text-muted-foreground'
const errorClass = 'text-xs font-medium text-destructive'

const TEXT_FIELDS: { field: Exclude<Field, 'coverImageUrl'>; label: string; placeholder: string; max: number; type?: string; autocomplete?: string }[] = [
  { field: 'name', label: 'Marka adı', placeholder: 'ör. Aegean Blue Hotel', max: 120 },
  { field: 'industry', label: 'Sektör', placeholder: 'ör. Butik otel', max: 80 },
  { field: 'contactName', label: 'İlgili kişi', placeholder: 'Ad Soyad', max: 120, autocomplete: 'name' },
  { field: 'contactEmail', label: 'E-posta', placeholder: 'ad@marka.com', max: 254, type: 'email', autocomplete: 'email' },
]
</script>

<template>
  <Dialog v-model:open="state.open">
    <DialogContent
      :show-close-button="false"
      class="max-h-[calc(100dvh-2rem)] gap-0 overflow-y-auto p-0 sm:max-w-[620px]"
    >
      <div class="flex items-start justify-between gap-4 px-5 pt-6 pb-1.5 sm:px-[26px]">
        <div>
          <DialogTitle class="text-[22px] font-extrabold tracking-[-0.01em]">Yeni Marka</DialogTitle>
          <DialogDescription class="mt-1 text-[13px] text-muted-foreground">
            Ad, sektör ve iletişim bilgisi yeterli. Kapak isteğe bağlı.
          </DialogDescription>
        </div>
        <DialogClose as-child>
          <Button variant="secondary" size="icon" class="size-[34px] shrink-0" aria-label="Kapat">
            <XIcon />
          </Button>
        </DialogClose>
      </div>

      <form id="new-client-form" class="grid gap-4 px-5 py-4 sm:grid-cols-2 sm:px-[26px]" novalidate @submit.prevent="submit">
        <div v-for="f in TEXT_FIELDS" :key="f.field" class="grid gap-1.5">
          <Label :for="id(f.field)" :class="labelClass">{{ f.label }}</Label>
          <Input
            :id="id(f.field)"
            v-model="form[f.field]"
            :type="f.type ?? 'text'"
            :maxlength="f.max"
            :autocomplete="f.autocomplete ?? 'off'"
            :placeholder="f.placeholder"
            :aria-invalid="!!errors[f.field]"
            :aria-describedby="describedBy(f.field)"
          />
          <p v-if="errors[f.field]" :id="`${id(f.field)}-error`" :class="errorClass">{{ errors[f.field] }}</p>
        </div>

        <!-- Native radios: arrow keys move between covers, Space selects. -->
        <fieldset
          :id="id('coverImageUrl')"
          class="grid gap-2 sm:col-span-2"
          :aria-describedby="describedBy('coverImageUrl')"
        >
          <legend :class="cn(labelClass, 'mb-2')">Kapak görseli <span :class="optional">(isteğe bağlı)</span></legend>
          <div class="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
            <label
              v-for="cover in [{ url: '', label: 'Kapak yok' }, ...COVER_IMAGES]"
              :key="cover.url"
              :class="cn(
                'group relative block aspect-[16/10] cursor-pointer overflow-hidden rounded-[10px] border-2 transition-colors',
                'has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50',
                form.coverImageUrl === cover.url ? 'border-coral' : 'border-transparent hover:border-coral/40',
              )"
            >
              <input v-model="form.coverImageUrl" type="radio" name="cover" :value="cover.url" class="sr-only" />
              <span class="sr-only">{{ cover.label }}</span>
              <BrandCover
                :name="form.name || 'Yeni Marka'"
                :url="cover.url || null"
                class="absolute inset-0 size-full"
                initials-class="text-xl"
              />
              <span v-if="!cover.url" class="absolute inset-x-0 bottom-1.5 text-center text-[11px] font-semibold text-coral-text" aria-hidden="true">
                Kapak yok
              </span>
              <span
                v-if="form.coverImageUrl === cover.url"
                class="absolute top-1.5 right-1.5 flex size-5 items-center justify-center rounded-full bg-coral text-white shadow-card"
                aria-hidden="true"
              >
                <CheckIcon class="size-3" :stroke-width="3" />
              </span>
            </label>
          </div>
          <p v-if="errors.coverImageUrl" :id="`${id('coverImageUrl')}-error`" :class="errorClass">{{ errors.coverImageUrl }}</p>
        </fieldset>
      </form>

      <div class="flex items-center justify-between gap-3 border-t border-divider bg-background px-5 py-4 sm:px-[26px]">
        <span class="hidden text-xs text-muted-foreground sm:inline">Esc ile kapat · Enter ile kaydet</span>
        <div class="ml-auto flex gap-2.5">
          <Button type="button" variant="outline" :disabled="saving" @click="state.open = false">Vazgeç</Button>
          <Button type="submit" form="new-client-form" :disabled="saving">
            {{ saving ? 'Kaydediliyor…' : 'Markayı kaydet' }}
          </Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

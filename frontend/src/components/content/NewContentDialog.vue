<script setup lang="ts">
import { XIcon } from '@lucide/vue'
import { ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { contentsApi } from '@/api'
import ChannelSelect from '@/components/content/form/ChannelSelect.vue'
import TypePicker from '@/components/content/form/TypePicker.vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { useClientOptions } from '@/composables/useClientOptions'
import { notifyContentSaved, useContentDialog } from '@/composables/useContentDialog'
import { useContentForm } from '@/composables/useContentForm'
import { refreshNavCounts } from '@/composables/useNavCounts'
import { errorMessage } from '@/lib/errors'

const { state } = useContentDialog()
const { clients } = useClientOptions()
const { form, errors, reset, validate, focusFirstError, applyServerErrors, body, id, describedBy, onKeydown } =
  useContentForm('new-content')

const saving = ref(false)

watch(() => state.newOpen, (open) => open && reset(null, { clientId: state.clientId }))

async function submit() {
  if (saving.value) return
  if (!validate()) return focusFirstError()
  saving.value = true
  try {
    const saved = await contentsApi.create(body())
    toast.success('İçerik taslak olarak oluşturuldu', { description: saved.title })
    notifyContentSaved(saved)
    void refreshNavCounts()
    state.newOpen = false
  } catch (e) {
    if (!applyServerErrors(e)) toast.error('İçerik kaydedilemedi', { description: errorMessage(e) })
  } finally {
    saving.value = false
  }
}

/** Start in the title field instead of the close button, the first focusable element. */
function focusTitle(event: Event) {
  event.preventDefault()
  document.getElementById(id('title'))?.focus()
}

const labelClass = 'text-sm font-semibold'
const optional = 'font-normal text-muted-foreground'
</script>

<template>
  <Dialog v-model:open="state.newOpen">
    <DialogContent
      :show-close-button="false"
      class="max-h-[calc(100dvh-2rem)] gap-0 overflow-y-auto p-0 sm:max-w-[620px]"
      @open-auto-focus="focusTitle"
    >
      <div class="flex items-start justify-between gap-4 px-5 pt-6 pb-1.5 sm:px-[26px]">
        <div>
          <DialogTitle class="text-[22px] font-extrabold tracking-[-0.01em]">Yeni İçerik</DialogTitle>
          <DialogDescription class="mt-1 text-[13px] text-muted-foreground">
            Yeni içerik her zaman taslak olarak başlar.
          </DialogDescription>
        </div>
        <DialogClose as-child>
          <Button variant="secondary" size="icon" class="size-[34px] shrink-0" aria-label="Kapat">
            <XIcon />
          </Button>
        </DialogClose>
      </div>

      <form
        id="new-content-form"
        class="grid gap-4 px-5 py-4 sm:grid-cols-2 sm:px-[26px]"
        novalidate
        @submit.prevent="submit"
        @keydown="(e: KeyboardEvent) => onKeydown(e, submit)"
      >
        <div class="grid gap-1.5 sm:col-span-2">
          <Label :for="id('title')" :class="labelClass">Başlık</Label>
          <Input
            :id="id('title')"
            v-model="form.title"
            maxlength="200"
            autocomplete="off"
            placeholder="ör. Gün batımı terası reels"
            :aria-invalid="!!errors.title"
            :aria-describedby="describedBy('title')"
          />
          <p v-if="errors.title" :id="`${id('title')}-error`" class="text-xs font-medium text-destructive">{{ errors.title }}</p>
        </div>

        <div class="grid gap-1.5">
          <Label :for="id('clientId')" :class="labelClass">Marka</Label>
          <Select v-model="form.clientId">
            <SelectTrigger
              :id="id('clientId')"
              class="w-full"
              :aria-invalid="!!errors.clientId"
              :aria-describedby="describedBy('clientId')"
            >
              <SelectValue placeholder="Marka seçin" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="client in clients" :key="client.id" :value="String(client.id)">{{ client.name }}</SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.clientId" :id="`${id('clientId')}-error`" class="text-xs font-medium text-destructive">{{ errors.clientId }}</p>
        </div>

        <div class="grid gap-1.5">
          <span :class="labelClass" class="leading-none">Tür</span>
          <TypePicker
            :id="id('type')"
            v-model="form.type"
            :invalid="!!errors.type"
            :described-by="describedBy('type')"
          />
          <p v-if="errors.type" :id="`${id('type')}-error`" class="text-xs font-medium text-destructive">{{ errors.type }}</p>
        </div>

        <div class="grid gap-1.5">
          <Label :for="id('channel')" :class="labelClass">Kanal <span :class="optional">(isteğe bağlı)</span></Label>
          <ChannelSelect
            :id="id('channel')"
            v-model="form.channel"
            :aria-invalid="!!errors.channel"
            :aria-describedby="describedBy('channel')"
          />
          <p v-if="errors.channel" :id="`${id('channel')}-error`" class="text-xs font-medium text-destructive">{{ errors.channel }}</p>
        </div>

        <div class="grid gap-1.5">
          <Label :for="id('dueDate')" :class="labelClass">Teslim tarihi</Label>
          <Input
            :id="id('dueDate')"
            v-model="form.dueDate"
            type="date"
            class="focus:border-ring focus:ring-3 focus:ring-ring/50"
            :aria-invalid="!!errors.dueDate"
            :aria-describedby="describedBy('dueDate')"
          />
          <p v-if="errors.dueDate" :id="`${id('dueDate')}-error`" class="text-xs font-medium text-destructive">{{ errors.dueDate }}</p>
        </div>

        <div class="grid gap-1.5 sm:col-span-2">
          <Label :for="id('notes')" :class="labelClass">Notlar <span :class="optional">(isteğe bağlı)</span></Label>
          <Textarea
            :id="id('notes')"
            v-model="form.notes"
            rows="3"
            maxlength="5000"
            placeholder="Brif, referanslar, onay notları…"
            :aria-invalid="!!errors.notes"
            :aria-describedby="describedBy('notes')"
          />
          <p v-if="errors.notes" :id="`${id('notes')}-error`" class="text-xs font-medium text-destructive">{{ errors.notes }}</p>
        </div>
      </form>

      <div class="flex items-center justify-between gap-3 border-t border-divider bg-background px-5 py-4 sm:px-[26px]">
        <span class="hidden text-xs text-muted-foreground sm:inline">Esc ile kapat · Enter ile kaydet</span>
        <div class="ml-auto flex gap-2.5">
          <Button type="button" variant="outline" :disabled="saving" @click="state.newOpen = false">Vazgeç</Button>
          <Button type="submit" form="new-content-form" :disabled="saving">
            {{ saving ? 'Kaydediliyor…' : 'Taslak olarak kaydet' }}
          </Button>
        </div>
      </div>
    </DialogContent>
  </Dialog>
</template>

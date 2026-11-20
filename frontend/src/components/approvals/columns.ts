/**
 * Desktop columns of the Onay Süreci list, shared by the heading row, the skeleton and the rows:
 * İçerik ~38%, Marka ~20%, Bekleme ~16%, Teslim ~14%, and the actions get the rest, but never
 * less than the two buttons need (on narrower screens the text columns give way proportionally).
 */
export const QUEUE_COLUMNS =
  'md:grid md:grid-cols-[minmax(0,38fr)_minmax(0,20fr)_minmax(0,16fr)_minmax(0,14fr)_minmax(176px,12fr)] md:items-center md:gap-4'

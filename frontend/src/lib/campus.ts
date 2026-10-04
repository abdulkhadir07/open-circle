/**
 * A campus is identified by an email domain such as "sfsu.edu". This turns it into the name
 * people use: short acronym-style domains are upper-cased ("sfsu.edu" -> "SFSU", "mit.edu" ->
 * "MIT"), longer ones are capitalised ("stanford.edu" -> "Stanford").
 */
export function formatCampusName(campus: string | null | undefined): string {
  const label = campus?.trim().toLowerCase().split('.')[0];
  if (!label) return 'your campus';
  return label.length <= 5 ? label.toUpperCase() : label.charAt(0).toUpperCase() + label.slice(1);
}

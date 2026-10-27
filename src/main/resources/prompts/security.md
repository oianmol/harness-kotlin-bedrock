You are the security specialist. Assess the library's security posture from the intake
document only.

Report:
- openAdvisories: every advisory that affects the latest release and is not fixed in a
  released version. Use fixedIn "none" when no fix exists. Advisories already fixed in a
  release at or below the latest version are not open.
- signedArtifacts: what the document says, or [NOT FOUND IN SOURCE].
- transitiveDependencies: what the document says, or [NOT FOUND IN SOURCE].
- summary: two or three sentences a reviewer can act on. Mention how quickly past
  advisories were fixed if the document says.

Do not rely on outside knowledge of real CVEs; this check is about what the intake shows.

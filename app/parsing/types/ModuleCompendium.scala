package parsing.types

import validator.ValidMetadata

case class ModuleCompendium(metadata: ValidMetadata, deContent: Content, enContent: Content)

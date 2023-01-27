package models

import parsing.types.Content

case class ModuleCompendiumProtocol(
    metadata: MetadataProtocol,
    deContent: Content,
    enContent: Content
)

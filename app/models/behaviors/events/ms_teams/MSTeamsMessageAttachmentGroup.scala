package models.behaviors.events.ms_teams

import models.behaviors.events.MessageAttachmentGroup

trait MSTeamsMessageAttachmentGroup extends MessageAttachmentGroup {
  type T = MSTeamsMessageAttachment
}

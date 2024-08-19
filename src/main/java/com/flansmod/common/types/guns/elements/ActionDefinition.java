package com.flansmod.common.types.guns.elements;

import com.flansmod.common.types.JsonField;
import com.flansmod.common.types.elements.ModifierDefinition;
import com.flansmod.common.types.elements.SoundDefinition;

public class ActionDefinition
{
	public static final ActionDefinition Invalid = new ActionDefinition();
	public boolean IsValid() { return actionType != EActionType.Invalid; }

	// General fields
	@JsonField
	public EActionType actionType = EActionType.Invalid;

	@JsonField(Docs = "In seconds", Min = 0.0f)
	public float duration = 0.0f;

	@JsonField
	public SoundDefinition[] sounds = new SoundDefinition[0];
	@JsonField
	public String itemStack = "";

	// IronSight / Scope Action
	@JsonField
	public String scopeOverlay = "";

	// Animation action specifics
	@JsonField
	public String anim = "";
}

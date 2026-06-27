package com.floently.learn.features

import com.floently.learn.navigation.LearnFeatureDestination

data class LearnFeatureContract(
    val destination: LearnFeatureDestination,
    val parityRequirements: List<LearnParityRequirement>,
    val accessRequirements: List<LearnAccessRequirement>,
    val interactionRequirements: List<LearnInteractionRequirement>,
    val releaseRequirements: List<LearnReleaseRequirement>
) {
    val isReleaseBlocked: Boolean
        get() = releaseRequirements.isNotEmpty()
}

enum class LearnParityRequirement {
    ExistingContentParity,
    ExistingProgressParity,
    ExistingAnimationParity,
    ExistingAudioParity,
    ExistingConversationParity,
    ExistingSubscriptionParity,
    ExistingDeviceLimitParity,
    ExistingLanguageParity,
    ExistingSettingsParity
}

enum class LearnAccessRequirement {
    RequiresLearnEntitlement,
    RequiresSignedInSession,
    RequiresDeviceAllowance,
    RequiresSubscriptionState
}

enum class LearnInteractionRequirement {
    NativeNavigation,
    NativeAnimations,
    OfflineSafeState,
    ErrorRecovery,
    BeginnerSafeFlow,
    AntiRepetitionFlow,
    LanguageSelectionFlow
}

enum class LearnReleaseRequirement {
    ProductParityAudit,
    BackendContractAudit,
    SubscriptionAudit,
    DeviceLimitAudit,
    InteractionAudit,
    LanguageAudit,
    NativeQaPass
}

object LearnFeatureContracts {
    val all: Map<LearnFeatureDestination, LearnFeatureContract> = LearnFeatureDestination.primary.associateWith { destination ->
        when (destination) {
            LearnFeatureDestination.YkiPractice -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingContentParity,
                    LearnParityRequirement.ExistingProgressParity,
                    LearnParityRequirement.ExistingAnimationParity,
                    LearnParityRequirement.ExistingAudioParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.NativeAnimations,
                    LearnInteractionRequirement.OfflineSafeState,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.ProfessionalFinnish -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingContentParity,
                    LearnParityRequirement.ExistingProgressParity,
                    LearnParityRequirement.ExistingAnimationParity,
                    LearnParityRequirement.ExistingAudioParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.NativeAnimations,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.Roleplay -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingConversationParity,
                    LearnParityRequirement.ExistingProgressParity,
                    LearnParityRequirement.ExistingAnimationParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.NativeAnimations,
                    LearnInteractionRequirement.BeginnerSafeFlow,
                    LearnInteractionRequirement.AntiRepetitionFlow,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.Cards -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingContentParity,
                    LearnParityRequirement.ExistingProgressParity,
                    LearnParityRequirement.ExistingAnimationParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.NativeAnimations,
                    LearnInteractionRequirement.OfflineSafeState,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.Progress -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingProgressParity,
                    LearnParityRequirement.ExistingSubscriptionParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.Settings -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingLanguageParity,
                    LearnParityRequirement.ExistingSettingsParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.LanguageSelectionFlow,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
            LearnFeatureDestination.Account -> LearnFeatureContract(
                destination = destination,
                parityRequirements = listOf(
                    LearnParityRequirement.ExistingSubscriptionParity,
                    LearnParityRequirement.ExistingDeviceLimitParity
                ),
                accessRequirements = defaultAccessRequirements(),
                interactionRequirements = listOf(
                    LearnInteractionRequirement.NativeNavigation,
                    LearnInteractionRequirement.ErrorRecovery
                ),
                releaseRequirements = defaultReleaseRequirements()
            )
        }
    }

    fun require(destination: LearnFeatureDestination): LearnFeatureContract =
        all.getValue(destination)

    private fun defaultAccessRequirements(): List<LearnAccessRequirement> = listOf(
        LearnAccessRequirement.RequiresSignedInSession,
        LearnAccessRequirement.RequiresLearnEntitlement,
        LearnAccessRequirement.RequiresDeviceAllowance,
        LearnAccessRequirement.RequiresSubscriptionState
    )

    private fun defaultReleaseRequirements(): List<LearnReleaseRequirement> = listOf(
        LearnReleaseRequirement.ProductParityAudit,
        LearnReleaseRequirement.BackendContractAudit,
        LearnReleaseRequirement.SubscriptionAudit,
        LearnReleaseRequirement.DeviceLimitAudit,
        LearnReleaseRequirement.InteractionAudit,
        LearnReleaseRequirement.LanguageAudit,
        LearnReleaseRequirement.NativeQaPass
    )
}

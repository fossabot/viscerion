package com.wireguard.android;

import android.databinding.ObservableList;

import com.wireguard.config.Profile;

/**
 * Interface for the background connection service.
 */

public interface ProfileServiceInterface {
    /**
     * Attempt to set up and enable an interface for this profile. The profile's connection state
     * will be updated if connection is successful. If this profile is already connected, or it is
     * not a known profile, no changes will be made.
     *
     * @param profile The profile (in the list of known profiles) to use for this connection.
     */
    void connectProfile(Profile profile);

    /**
     * Creates a deep copy of an existing profile that can be modified and then passed to
     * saveProfile. If the given profile is not a known profile, or the profile cannot be copied,
     * this function returns null.
     *
     * @param profile The existing profile (in the list of known profiles) to copy.
     * @return A copy of the profile that can be freely modified.
     */
    Profile copyProfileForEditing(Profile profile);

    /**
     * Attempt to disable and tear down an interface for this profile. The profile's connection
     * state will be updated if disconnection is successful. If this profile is already
     * disconnected, or it is not a known profile, no changes will be made.
     *
     * @param profile The profile (in the list of known profiles) to disconnect.
     */
    void disconnectProfile(Profile profile);

    /**
     * Retrieve a list of profiles known and managed by this service. Profiles in this list must not
     * be modified directly. If a profile is to be updated, first create a copy of it by calling
     * copyProfileForEditing().
     *
     * @return The list of known profiles.
     */
    ObservableList<Profile> getProfiles();

    /**
     * Remove a profile from being managed by this service. If the profile is currently connected,
     * it will be disconnected before it is removed. If successful, configuration for this profile
     * will be removed from persistent storage. If the profile is not a known profile, no changes
     * will be made.
     *
     * @param profile The profile (in the list of known profiles) to remove.
     */
    void removeProfile(Profile profile);

    /**
     * Replace the given profile, or add a new profile if oldProfile is null.
     * If the profile exists and is currently connected, it will be disconnected before the
     * replacement, and the service will attempt to reconnect it afterward. If the profile is new,
     * it will be set to the disconnected state. If successful, configuration for this profile will
     * be saved to persistent storage.
     *
     * @param oldProfile The existing profile to replace, or null to add the new profile.
     * @param newProfile The profile to add, or a copy of the profile to replace.
     */
    void saveProfile(Profile oldProfile, Profile newProfile);
}

package dev.tolkach.music.commands;

public sealed interface MusicCommand permits PlayTrackCommand, PauseCommand, ResumeCommand, NextCommand, PreviousCommand {

}

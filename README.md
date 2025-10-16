# Bansoukou
Bansōkō [絆創膏] - a simple coremod that streamlines patching of mods.

## Usage

1. Create a folder in `.minecraft` named `bansoukou`. You can do this manually or run with this mod in the instance for it to create one.
2. In `/bansoukou`, here is where you can define patches, where jar/zip files will represent the jars/zips. The names should be 1-to-1 to the mod files you are trying to patch.
3. Match the directory structure of the mod you are patching,  place whatever files you want replacing in the same relative paths
4. Signatures do not matter, Bansoukou will delete them at runtime.

*Note: If you want to delete a file from the jar, you simply place an empty file with the same relative path name and Bansoukou will detect that and remove the respective file from the jar at runtime.*

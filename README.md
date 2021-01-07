# Bansoukou
Bansōkō [絆創膏] - a simple coremod that streamlines patching of mods.

## Usage

1. Create a folder in `.minecraft` named `bansoukou`. You can do this manually or run with this mod in the instance for it to create one.
2. In `/bansoukou`, here is where you can define patches, where folders will represent the jar you want to patch with the names of the folders matching the jars in `mods/`.

It should look something like this: 

![RootFolderStructure](https://i.imgur.com/dt4QdXT.png)

3. Match the jar file's directory structure, and place whatever files you want replacing in the same relative paths, such as:

![InsideFolderStructure_BansoukouFolder](https://i.imgur.com/XlD1NLG.png)

With the jar looking like:

![InsideJarStructure](https://i.imgur.com/DS3HBs8.png)

*Note: If you want to delete a file from the jar, you simply place an empty file with the same relative path name and Bansoukou will detect that and remove the respective file from the jar at runtime.*

*Coremods: They... do not work yet (as the ClassLoader has already done its magic), but I do have a solution coming, but as I am concentrating on my own modpack I likely will not update this any time soon. It won't be as seamless and probably would need a restart.*

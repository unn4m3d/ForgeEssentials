package com.forgeessentials.core.preloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.forgeessentials.core.preloader.asminjector.ASMClassWriter;
import com.forgeessentials.core.preloader.asminjector.ASMUtil;
import com.forgeessentials.core.preloader.asminjector.ClassInjector;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;

public class EventTransformer implements ITransformer<ClassNode>
{

    public static final boolean isObfuscated = EventTransformer.class.getClassLoader() != Launcher.class.getClassLoader();

    private List<ClassInjector> injectors = new ArrayList<>();
    private Set<Target> targets = new HashSet<>();

    public EventTransformer(String[] injectors, String[] targets)
    {
        for (String clazz : injectors)
        {
            ClassInjector CIj = ClassInjector.create(clazz, isObfuscated);
            this.injectors.add(CIj);
        }

        for (String clazz : targets) {
            this.targets.add(Target.targetClass(clazz));
        }
    }

    @Nonnull @Override public ClassNode transform(ClassNode classNode, ITransformerVotingContext context)
    {
        // Apply transformers
        for (ClassInjector injector : injectors)
            injector.inject(classNode);

        return classNode;
    }

    @Nonnull @Override public TransformerVoteResult castVote(ITransformerVotingContext context)
    {

        return TransformerVoteResult.YES;
    }

    @Nonnull @Override public Set<Target> targets()
    {
        return targets;
    }
}
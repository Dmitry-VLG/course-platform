package ru.kobzev.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kobzev.platform.api.Inputs;
import ru.kobzev.platform.api.Views;
import ru.kobzev.platform.domain.*;
import ru.kobzev.platform.repository.*;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;

@Service
@Transactional(readOnly = true)
public class DirectoryService {
    private final Access access;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final CategoryRepository categories;
    private final TagRepository tags;

    public DirectoryService(Access access,
            UserRepository users,
            ProfileRepository profiles,
            CategoryRepository categories,
            TagRepository tags) {
        this.access = access;
        this.users = users;
        this.profiles = profiles;
        this.categories = categories;
        this.tags = tags;
    }

    public List<Views.Person> users() {
        return users.findAll(Sort.by("id")).stream().map(Views.Person::of).toList();
    }

    @Transactional
    public Views.Person saveUser(Integer id, Inputs.Person data) {
        User u = id == null ? new User() : access.get(User.class, id);
        if (id != null && u.getRole() != data.role()) {
            throw PlatformException.conflict("Роль существующего пользователя изменить нельзя");
        }
        u.setRole(data.role());
        u.setName(data.name().strip());
        u.setEmail(data.email().strip().toLowerCase(Locale.ROOT));
        return Views.Person.of(users.save(u));
    }

    public Views.Person user(int id) { return Views.Person.of(access.get(User.class, id)); }

    @Transactional
    public void deleteUser(int id) { users.delete(access.get(User.class, id)); }

    @Transactional
    public Views.ProfileView saveProfile(int id, Inputs.ProfileData data) {
        User user = access.get(User.class, id);
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        profile.setBio(data.bio());
        profile.setAvatarUrl(data.avatarUrl());
        profiles.save(profile);
        return profile(id);
    }

    public Views.ProfileView profile(int id) {
        Profile p = access.get(User.class, id).getProfile();
        if (p == null) throw PlatformException.missing("Профиль не заполнен");
        return new Views.ProfileView(p.getId(), id, p.getBio(), p.getAvatarUrl());
    }

    public List<Views.Named> categories() {
        return categories.findAll(Sort.by("name")).stream().map(x -> new Views.Named(x.getId(), x.getName())).toList();
    }

    @Transactional
    public Views.Named saveCategory(Integer id, Inputs.Name data) {
        Category item = id == null ? new Category() : access.get(Category.class, id);
        item.setName(data.name().strip());
        categories.save(item);
        return new Views.Named(item.getId(), item.getName());
    }

    @Transactional
    public void deleteCategory(int id) { categories.delete(access.get(Category.class, id)); }

    public List<Views.Named> tags() {
        return tags.findAll(Sort.by("name")).stream().map(x -> new Views.Named(x.getId(), x.getName())).toList();
    }

    @Transactional
    public Views.Named saveTag(Integer id, Inputs.Name data) {
        Tag item = id == null ? new Tag() : access.get(Tag.class, id);
        item.setName(data.name().strip());
        tags.save(item);
        return new Views.Named(item.getId(), item.getName());
    }

    @Transactional
    public void deleteTag(int id) { tags.delete(access.get(Tag.class, id)); }
}

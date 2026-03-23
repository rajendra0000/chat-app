"use client";

// ==============================
// Profile Settings Page
// ==============================

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { profileSchema, type ProfileFormValues } from "@/lib/validators";
import {
  fetchUserProfile,
  updateUserProfile,
  getMyProfilePicUrl,
  uploadProfilePic,
} from "@/services/user-service";
import { useAuthStore } from "@/store/auth-store";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ArrowLeft, Camera, Loader2, Save, User } from "lucide-react";
import { toast } from "sonner";
import { USER_PROFILE_STALE_TIME } from "@/lib/constants";

export default function SettingsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const [isUploadingPic, setIsUploadingPic] = useState(false);

  // Fetch profile
  const { data: profile, isLoading: isLoadingProfile } = useQuery({
    queryKey: ["userProfile"],
    queryFn: fetchUserProfile,
    staleTime: USER_PROFILE_STALE_TIME,
  });

  // Fetch profile pic
  const { data: picUrl } = useQuery({
    queryKey: ["myProfilePic"],
    queryFn: getMyProfilePicUrl,
    staleTime: USER_PROFILE_STALE_TIME,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
  });

  // Populate form when profile loads
  useEffect(() => {
    if (profile) {
      reset({
        fullName: profile.fullName || "",
        dateOfBirth: profile.dateOfBirth || "",
        gender: profile.gender || "",
        address: profile.address || "",
      });
    }
  }, [profile, reset]);

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: (data: ProfileFormValues) =>
      updateUserProfile({
        fullName: data.fullName,
        dateOfBirth: data.dateOfBirth || "",
        gender: data.gender || "",
        address: data.address || "",
      }),
    onSuccess: (updatedUser) => {
      setUser(updatedUser);
      queryClient.invalidateQueries({ queryKey: ["userProfile"] });
      queryClient.invalidateQueries({ queryKey: ["currentUser"] });
      toast.success("Profile updated!");
    },
    onError: () => {
      toast.error("Failed to update profile.");
    },
  });

  const handlePicUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsUploadingPic(true);
    try {
      await uploadProfilePic(file);
      queryClient.invalidateQueries({ queryKey: ["myProfilePic"] });
      toast.success("Profile picture updated!");
    } catch {
      toast.error("Failed to upload picture.");
    } finally {
      setIsUploadingPic(false);
    }
  };

  const userName = user?.fullName || profile?.fullName || "User";
  const initials = userName
    .split(" ")
    .map((n) => n[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="h-16 px-4 flex items-center gap-3 border-b border-border shrink-0">
        <Button variant="ghost" size="icon" onClick={() => router.push("/chat")}>
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <User className="w-5 h-5 text-accent" />
        <h1 className="text-lg font-semibold">Profile Settings</h1>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-6">
        <div className="max-w-md mx-auto space-y-8">
          {/* Profile picture */}
          <div className="flex flex-col items-center">
            <div className="relative group">
              <Avatar className="h-24 w-24">
                <AvatarImage src={picUrl || undefined} alt={userName} />
                <AvatarFallback className="bg-accent/20 text-accent text-2xl font-bold">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <label className="absolute inset-0 flex items-center justify-center bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
                {isUploadingPic ? (
                  <Loader2 className="w-6 h-6 text-white animate-spin" />
                ) : (
                  <Camera className="w-6 h-6 text-white" />
                )}
                <input
                  type="file"
                  className="hidden"
                  accept="image/*"
                  onChange={handlePicUpload}
                  disabled={isUploadingPic}
                />
              </label>
            </div>
            <p className="text-sm text-muted-foreground mt-2">
              Click to change photo
            </p>
          </div>

          {/* Profile form */}
          {isLoadingProfile ? (
            <div className="flex justify-center py-8">
              <Loader2 className="w-6 h-6 animate-spin text-accent" />
            </div>
          ) : (
            <form onSubmit={handleSubmit((data) => updateMutation.mutate(data))} className="space-y-5">
              <div className="space-y-1.5">
                <Label htmlFor="settings-name">Full Name</Label>
                <Input
                  {...register("fullName")}
                  id="settings-name"
                  className="h-11 rounded-xl"
                />
                {errors.fullName && (
                  <p className="text-destructive text-xs">{errors.fullName.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="settings-dob">Date of Birth</Label>
                  <Input
                    {...register("dateOfBirth")}
                    id="settings-dob"
                    type="date"
                    className="h-11 rounded-xl"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="settings-gender">Gender</Label>
                  <select
                    {...register("gender")}
                    id="settings-gender"
                    className="flex h-11 w-full rounded-xl border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <option value="">Select</option>
                    <option value="male">Male</option>
                    <option value="female">Female</option>
                    <option value="other">Other</option>
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="settings-address">Address</Label>
                <Input
                  {...register("address")}
                  id="settings-address"
                  className="h-11 rounded-xl"
                />
              </div>

              <div className="space-y-1.5">
                <Label>Phone</Label>
                <Input
                  value={user?.phone || ""}
                  disabled
                  className="h-11 rounded-xl opacity-60"
                />
                <p className="text-xs text-muted-foreground">Phone number cannot be changed.</p>
              </div>

              <Button
                type="submit"
                size="lg"
                className="w-full rounded-xl"
                disabled={updateMutation.isPending || !isDirty}
              >
                {updateMutation.isPending ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4" />
                    Save Changes
                  </>
                )}
              </Button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
